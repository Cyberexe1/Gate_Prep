package com.gateprep.api.auth;

import com.gateprep.api.auth.AuthDtos.AuthResponse;
import com.gateprep.api.auth.AuthDtos.UserDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    /** A real hash used to spend the same time when the e-mail does not exist (avoids revealing which e-mails are registered). */
    private final String dummyHash;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginRateLimiter limiter;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens, LoginRateLimiter limiter) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.limiter = limiter;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthResponse register(String email, String password, String name) {
        String e = normalize(email);
        if (users.existsByEmail(e)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists. Please sign in.");
        }
        UserAccount user = new UserAccount(UUID.randomUUID(), e, name.trim(), encoder.encode(password));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException dup) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists. Please sign in.");
        }
        return issue(user);
    }

    @Transactional
    public AuthResponse login(String email, String password, String clientIp) {
        String e = normalize(email);
        String key = clientIp + "|" + e;
        limiter.check(key);
        UserAccount user = users.findByEmail(e).orElse(null);
        boolean ok = encoder.matches(password, user != null ? user.getPasswordHash() : dummyHash) && user != null;
        if (!ok) {
            limiter.failed(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password.");
        }
        limiter.succeeded(key);
        return issue(user);
    }

    // noRollbackFor: when a re-used token is detected, revoking all of the user's tokens must still be committed
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        UUID userId = tokens.consume(rawRefreshToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Please sign in again."));
        UserAccount user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Please sign in again."));
        return issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokens.revoke(rawRefreshToken);
    }

    public UserDto me(UUID id) {
        UserAccount u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName());
    }

    private AuthResponse issue(UserAccount user) {
        return new AuthResponse(
            new UserDto(user.getId(), user.getEmail(), user.getDisplayName()),
            tokens.accessToken(user), tokens.newRefreshToken(user.getId()), tokens.accessTtlSeconds());
    }
}
