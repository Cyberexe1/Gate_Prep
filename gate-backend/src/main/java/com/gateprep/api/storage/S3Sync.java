package com.gateprep.api.storage;

import com.gateprep.api.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** One-off upload of <content-dir>/images to the bucket (set S3_SYNC_ON_START=true once). Skips files already there. */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3Sync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(S3Sync.class);

    private final AppProps props;

    public S3Sync(AppProps props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!props.storage().syncToS3()) return;
        AppProps.S3 cfg = props.storage().s3();
        Path root = Path.of(props.contentDir(), "images").toAbsolutePath().normalize();
        try (S3Client s3 = S3Client.builder().region(Region.of(cfg.region())).credentialsProvider(S3ImageStore.credentials(cfg)).build()) {
            Set<String> existing = new HashSet<>();
            s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(cfg.bucket()).prefix(cfg.prefix()).build())
                .contents().forEach((S3Object o) -> existing.add(o.key()));
            int uploaded = 0;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path p : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                    String key = cfg.prefix() + root.relativize(p).toString().replace('\\', '/');
                    if (existing.contains(key)) continue;
                    s3.putObject(PutObjectRequest.builder().bucket(cfg.bucket()).key(key).contentType("image/webp")
                        .cacheControl("private, max-age=86400").build(), RequestBody.fromFile(p));
                    uploaded++;
                }
            }
            log.info("S3 sync done: {} uploaded, {} already present", uploaded, existing.size());
        }
    }
}
