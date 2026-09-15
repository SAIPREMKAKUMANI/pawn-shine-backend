package com.project.pawn.billing.controller;

import com.project.pawn.billing.dto.response.ItemImageResponseDto;
import com.project.pawn.billing.model.ItemImage;
import com.project.pawn.billing.repository.ItemImageRepository;
import com.project.pawn.billing.service.ImageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemImageController {

    private static final Path BASE_PATH_ITEM = Path.of("/projects/pawn-images/items");
    private final ImageHandler imageHandler;
    private final ItemImageRepository itemImageRepository;

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ItemImageResponseDto>> getItemImages(@PathVariable(name = "id") Long itemId) {
        log.info("Fetching item images for itemId={}", itemId);
        return ResponseEntity.ok(imageHandler.fetchItemImages(itemId));
    }

    @GetMapping("/images/serve/{imageId}")
    public ResponseEntity<Resource> serveItemImage(@PathVariable Long imageId) {
        log.info("Serving item image for imageId={}", imageId);
        Optional<ItemImage> itemImageOpt = itemImageRepository.findById(imageId);
        if (itemImageOpt.isEmpty()) {
            log.warn("Item image not found for ID: {}", imageId);
            return ResponseEntity.notFound().build();
        }

        ItemImage itemImage = itemImageOpt.get();
        String imageUrlStr = itemImage.getImageUrl();
        if (imageUrlStr == null || imageUrlStr.isBlank()) {
            log.warn("Image URL path is empty for image ID: {}", imageId);
            return ResponseEntity.notFound().build();
        }

        Path filePath = Path.of(imageUrlStr).normalize();
        Path basePathAbs = BASE_PATH_ITEM.toAbsolutePath();
        Path filePathAbs = filePath.toAbsolutePath();

        // Path traversal guard
        if (!filePathAbs.startsWith(basePathAbs)) {
            log.warn("Path traversal attempt or invalid image path for image ID {}: {}", imageId, imageUrlStr);
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("Item image file not found on disk: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(resource);

        } catch (IOException e) {
            log.error("Error reading item image file for image ID {}: {}", imageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

