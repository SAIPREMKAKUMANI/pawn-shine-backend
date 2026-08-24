package com.project.pawn.customeronboarding.util;

import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.enums.MediaType;
import com.project.pawn.customeronboarding.exception.GenericCustomerOnboardingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static com.project.pawn.customeronboarding.enums.MediaType.PROFILE;
import static com.project.pawn.customeronboarding.enums.MediaType.RELATIVE;

@Slf4j
@Service
public class ImageHandlingService {

    private static final Path BASE_PATH = Path.of("/projects/pawn-images");

    public void uploadAllImagesToDisk(CustomerDto request, Long custId) {
        log.info("Uploading images for customer: {}", request.getName());

        String profileImageUrl = uploadImageSafe(request.getImage(), custId, PROFILE);
        if (profileImageUrl != null) {
            request.setImageUrl(profileImageUrl);
        }

        if (request.getIdProofs() != null) {
            request.getIdProofs().forEach(idProof -> {
                String proofImageUrl = uploadImageSafe(idProof.getImage(), custId, idProof.getIdType());
                if (proofImageUrl != null) {
                    idProof.setImageUrl(proofImageUrl);
                }
            });
        }

        if (request.getRelatives() != null) {
            request.getRelatives().forEach(relative -> {
                String relImageUrl = uploadImageSafe(relative.getImage(), custId, RELATIVE);
                if (relImageUrl != null) {
                    relative.setImageUrl(relImageUrl);
                }
            });
        }

        log.info("All images uploaded successfully for customer: {}", request.getName());
    }

    @Retryable(
            retryFor = {IOException.class},
            backoff = @Backoff(delay = 2000)
    )
    public String uploadImage(MultipartFile file, Long custId, MediaType type) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("No file provided for customer ID {} type {}", custId, type);
            return null;
        }

        Path uploadPath = BASE_PATH.resolve(String.valueOf(custId));
        String fileExtension = resolveFileExtension(file);
        Path filePath = uploadPath.resolve(type.name() + "." + fileExtension);

        try {
            Files.createDirectories(uploadPath);

            if (!Files.exists(uploadPath)) {
                log.error("Directory does not exist after creation attempt: {}", uploadPath);
                throw new GenericCustomerOnboardingException("Directory does not exist to store images");
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully uploaded {} image for customer ID {} at {}", type, custId, filePath);
        } catch (InvalidPathException e) {
            log.error("Invalid path for customer ID {}: {}", custId, e.getMessage(), e);
            throw new GenericCustomerOnboardingException("Invalid file path during " + type + " image upload");
        }

        return "/api/images/" + custId + "/" + type.name() + "." + fileExtension;
    }

    /**
     * Wraps uploadImage to convert checked IOException to unchecked exception.
     * Used inside lambda expressions where checked exceptions cannot be thrown.
     */
    private String uploadImageSafe(MultipartFile file, Long custId, MediaType type) {
        try {
            return uploadImage(file, custId, type);
        } catch (IOException e) {
            log.error("I/O error while uploading {} image for customer ID {}: {}", type, custId, e.getMessage(), e);
            throw new GenericCustomerOnboardingException("Failed to upload " + type + " image: " + e.getMessage());
        }
    }

    private String resolveFileExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (Objects.nonNull(contentType) && contentType.contains("/")) {
            return contentType.substring(contentType.lastIndexOf('/') + 1);
        }
        return "png";
    }
}
