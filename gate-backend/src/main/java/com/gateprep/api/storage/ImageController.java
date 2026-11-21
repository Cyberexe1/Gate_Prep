package com.gateprep.api.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Question / solution / figure images. Needs a valid access token (the whole /api tree except /auth and /catalog does). */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Set<String> FOLDERS = Set.of("q", "s", "f");
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,100}\\.webp");

    private final ImageStore store;

    public ImageController(ImageStore store) {
        this.store = store;
    }

    @GetMapping("/{folder}/{name}")
    public ResponseEntity<?> image(@PathVariable String folder, @PathVariable String name) {
        if (!FOLDERS.contains(folder) || !NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such image");
        }
        var link = store.presignedUrl(folder, name);
        if (link.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link.get()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300").build();
        }
        Resource file = store.file(folder, name).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such image"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/webp"))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate()).body(file);
    }
}
