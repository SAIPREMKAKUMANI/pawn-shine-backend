package com.project.pawn.customeronboarding.controller;

import com.project.pawn.common.enums.MediaType;
import com.project.pawn.common.util.SecuritySanitizer;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.dto.IdProofDto;
import com.project.pawn.customeronboarding.dto.RelativeDto;
import com.project.pawn.customeronboarding.service.CustomerCacheHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class CustomerImageController {

    private static final Path BASE_PATH = Path.of("/projects/pawn-images/customer");
    private final CustomerCacheHandler customerCacheHandler;
    /**
     * Serves customer images from the filesystem.
     *
     * @param custId   the customer ID (maps to a subdirectory)
     * @param imageFile the image filetype (e.g. PROFILE, AADHAR, RELATIVE)
     * @return the image file with appropriate content type and cache headers
     */
    @GetMapping("/{custId}/{imageFile}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long custId,
            @PathVariable String imageFile) {

        imageFile = SecuritySanitizer.sanitizeInput(imageFile);
        if(StringUtils.isEmpty(imageFile)) {
            log.info("Filename is empty for customer ID {}", custId);
            return ResponseEntity.badRequest().build();
        }
        imageFile = imageFile.toUpperCase();

        List<String> splits = List.of(imageFile.split("_"));
        String fileType = splits.get(0);
        String fileId = splits.size() > 1 ? splits.get(1) : null;

        MediaType mediaType;
        try {
            mediaType = MediaType.valueOf(fileType);
        } catch (IllegalArgumentException e) {
            log.info("Invalid filetype {} for customer ID {}", fileType, custId);
            return ResponseEntity.badRequest().build();
        }

        Optional<CustomerDto> customer = customerCacheHandler.getCustomer(custId);
        if(customer.isEmpty()) {
            log.info("Customer with ID {} not found ", custId);
            return ResponseEntity.notFound().build();
        }

        String filename = switch (mediaType) {
            case PROFILE -> customer.get().getImageUrl();
            case RELATIVE -> customer.get().getRelatives().stream()
                    .filter(relative -> relative.getRelativeId().toString().equals(fileId))
                    .map(RelativeDto::getImageUrl)
                    .findFirst()
                    .orElse(null);
            case AADHAR, PAN, VOTER_ID, DRIVING_LICENSE -> customer.get().getIdProofs().stream()
                    .filter(idProof -> idProof.getIdProofId().toString().equals(fileId))
                    .map(IdProofDto::getImageUrl)
                    .findFirst()
                    .orElse(null);
            default -> null;
        };

        if(StringUtils.isEmpty(filename)) {
            log.info("No image URL found for customer ID {} and filetype {}", custId, imageFile);
            return ResponseEntity.notFound().build();
        }

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
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(resource);

        } catch (IOException e) {
            log.error("Error reading image file for customer ID {}: {}", custId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
