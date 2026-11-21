package com.gateprep.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** All custom settings (see application.yml; values come from environment variables or the .env file). */
@ConfigurationProperties(prefix = "app")
public record AppProps(String contentDir, Jwt jwt, Storage storage) {

    public record Jwt(String secret, int accessTtlMinutes, int refreshTtlDays) {}

    public record Storage(String type, boolean syncToS3, S3 s3) {}

    public record S3(String bucket, String region, String accessKey, String secretKey, String prefix, int presignMinutes) {}
}
