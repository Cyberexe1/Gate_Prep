package com.gateprep.api.storage;

import org.springframework.core.io.Resource;

import java.util.Optional;

/** Where the question and solution images live: files on disk (development) or an S3 bucket (production). */
public interface ImageStore {

    /** Image kinds are the sub-folders q (question), s (solution) and f (figure). */
    Optional<Resource> file(String folder, String name);

    /** A short-lived direct download link, or empty if this store serves the bytes itself. */
    Optional<String> presignedUrl(String folder, String name);
}
