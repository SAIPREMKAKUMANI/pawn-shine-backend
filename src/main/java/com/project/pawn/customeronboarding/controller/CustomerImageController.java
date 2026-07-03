package com.project.pawn.customeronboarding.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class CustomerImageController {

    private static final Path BASE_PATH = Path.of("/projects/pawn-images");

    /**
     * Serves customer images from the filesystem.
     *
     * @param custId   the customer ID (maps to a subdirectory)
     * @param filename the image filename (e.g. PROFILE.jpeg, AADHAR.png, RELATIVE.jpeg)
     * @return the image file with appropriate content type and cache headers
     */
    @GetMapping("/{custId}/{filename}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long custId,
            @PathVariable String filename) {

        Path filePath = BASE_PATH.resolve(String.valueOf(custId)).resolve(filename).normalize();

        // Guard against path traversal attacks
        if (!filePath.startsWith(BASE_PATH)) {
            log.warn("Path traversal attempt detected for customer ID {} with filename: {}", custId, filename);
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.info("Image not found: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(resource);

        } catch (IOException e) {
            log.error("Error reading image file for customer ID {}: {}", custId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
