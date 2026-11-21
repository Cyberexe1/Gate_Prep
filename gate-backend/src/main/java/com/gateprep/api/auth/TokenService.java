package com.gateprep.api.auth;

import com.gateprep.api.config.AppProps;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/** Short-lived JWT access tokens plus long-lived, rotating, hashed refresh tokens. */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final AppProps props;
    private final RefreshTokenRepository refreshTokens;

    public TokenService(JwtEncoder encoder, AppProps props, RefreshTokenRepository refreshTokens) {
        this.encoder = encoder;
        this.props = props;
        this.refreshTokens = refreshTokens;
    }

    public long accessTtlSeconds() {
        return Duration.ofMinutes(props.jwt().accessTtlMinutes()).toSeconds();
    }

    public String accessToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("gate-api")
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTtlSeconds()))
            .claim("email", user.getEmail())
            .claim("name", user.getDisplayName())
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** Creates and stores a new refresh token and returns the raw value (shown to the client once). */
    public String newRefreshToken(UUID userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(Duration.ofDays(props.jwt().refreshTtlDays()));
        refreshTokens.save(new RefreshToken(userId, sha256(raw), expires));
        return raw;
    }

    /**
     * Uses up a refresh token (rotation). Returns the user id if it was valid.
     * A token that was already used is treated as stolen: every token of that user is revoked.
     */
    public Optional<UUID> consume(String raw) {
        Optional<RefreshToken> found = refreshTokens.findByTokenHash(sha256(raw));
        if (found.isEmpty()) return Optional.empty();
        RefreshToken token = found.get();
        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(token.getUserId());
            return Optional.empty();
        }
        if (token.getExpiresAt().isBefore(Instant.now())) return Optional.empty();
        token.revoke();
        refreshTokens.save(token);
        return Optional.of(token.getUserId());
    }

    public void revoke(String raw) {
        refreshTokens.findByTokenHash(sha256(raw)).ifPresent(t -> {
            t.revoke();
            refreshTokens.save(t);
        });
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
