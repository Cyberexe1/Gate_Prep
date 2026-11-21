package com.gateprep.api.storage;

import com.gateprep.api.config.AppProps;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

/**
 * Images in S3. The client never gets bucket access: the API checks the user's token, then redirects to a
 * pre-signed link that expires after a few minutes.
 * Credentials come from AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY in .env. On ECS or EC2, leave them empty
 * and attach an IAM role to the task instead; the default credential chain is used.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ImageStore implements ImageStore {

    private final S3Presigner presigner;
    private final String bucket;
    private final String prefix;
    private final Duration ttl;

    public S3ImageStore(AppProps props) {
        AppProps.S3 s3 = props.storage().s3();
        if (s3.bucket() == null || s3.bucket().isBlank()) {
            throw new IllegalStateException("STORAGE_TYPE=s3 needs S3_BUCKET (and AWS credentials) in .env");
        }
        this.bucket = s3.bucket();
        this.prefix = s3.prefix() == null ? "images/" : s3.prefix();
        this.ttl = Duration.ofMinutes(s3.presignMinutes() > 0 ? s3.presignMinutes() : 10);
        this.presigner = S3Presigner.builder().region(Region.of(s3.region())).credentialsProvider(credentials(s3)).build();
    }

    static AwsCredentialsProvider credentials(AppProps.S3 s3) {
        if (s3.accessKey() != null && !s3.accessKey().isBlank() && s3.secretKey() != null && !s3.secretKey().isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Override
    public Optional<Resource> file(String folder, String name) {
        return Optional.empty();
    }

    @Override
    public Optional<String> presignedUrl(String folder, String name) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(prefix + folder + "/" + name).build();
        return Optional.of(presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build())
            .url().toString());
    }

    @PreDestroy
    void close() {
        presigner.close();
    }
}
