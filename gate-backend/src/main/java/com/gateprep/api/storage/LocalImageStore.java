package com.gateprep.api.storage;

import com.gateprep.api.config.AppProps;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStore implements ImageStore {

    private final Path root;

    public LocalImageStore(AppProps props) {
        this.root = Path.of(props.contentDir(), "images").toAbsolutePath().normalize();
    }

    @Override
    public Optional<Resource> file(String folder, String name) {
        Path p = root.resolve(folder).resolve(name).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) return Optional.empty();
        return Optional.of(new FileSystemResource(p));
    }

    @Override
    public Optional<String> presignedUrl(String folder, String name) {
        return Optional.empty();
    }
}
