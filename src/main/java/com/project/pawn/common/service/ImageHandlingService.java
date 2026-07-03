package com.project.pawn.common.service;

import com.project.pawn.common.enums.MediaType;
import com.project.pawn.common.exception.PawnBrokingException;
import com.project.pawn.common.exception.ErrorDetail;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.project.pawn.common.constants.Constants.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
public class ImageHandlingService {

    private static final Path BASE_PATH_CUSTOMER = Path.of("/projects/pawn-images/customer");
    private static final Path BASE_PATH_ITEM = Path.of("/projects/pawn-images/items");

    private static final String PLEDGE = "pledge";
    private static final String REDEEM = "redeem";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Retryable(
            retryFor = {IOException.class},
            backoff = @Backoff(delay = 2000)
    )
    public String uploadImage(MultipartFile file, Long id, MediaType type) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("No file provided for ID {} type {}", id, type);
            return null;
        }

        validateImageFile(file, type);

        Path uploadPath;
        if(type.equals(MediaType.PLEDGE_ITEM_IMAGE)) {
            uploadPath = BASE_PATH_ITEM.resolve(String.valueOf(id)).resolve(PLEDGE);
        } else if(type.equals(MediaType.REDEEM_ITEM_IMAGE)) {
            uploadPath = BASE_PATH_ITEM.resolve(String.valueOf(id)).resolve(REDEEM);
        } else {
            uploadPath = BASE_PATH_CUSTOMER.resolve(String.valueOf(id));
        }

        String fileExtension = resolveFileExtension(file);
        String uniqueName = type.name() + "_" + UUID.randomUUID() + "." + fileExtension;
        Path filePath = uploadPath.resolve(uniqueName);

        try {
            Files.createDirectories(uploadPath);

            if (!Files.exists(uploadPath)) {
                log.error("Directory does not exist after creation attempt: {}", uploadPath);
                throw new PawnBrokingException(INTERNAL_SERVER_ERROR);
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully uploaded {} image for ID {} at {}", type, id, filePath);
        } catch (InvalidPathException e) {
            log.error("Invalid path for ID {}: {}", id, e.getMessage(), e);
            throw new PawnBrokingException("Invalid file path during " + type + " image upload");
        }

        return "/api/images/" + id + "/" + uniqueName;
    }

    /**
     * Wraps uploadImage to convert checked IOException to unchecked exception.
     * Used inside lambda expressions where checked exceptions cannot be thrown.
     */
    public String uploadImageSafe(MultipartFile file, Long id, MediaType type) {
        try {
            return uploadImage(file, id, type);
        } catch (IOException e) {
            log.error("I/O error while uploading {} image for ID {}: {}", type, id, e.getMessage(), e);
            throw new PawnBrokingException("Failed to upload " + type + " image: " + e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file, MediaType type) {
        List<ErrorDetail> errorDetailsList = new ArrayList<>();
        if (file.getSize() > MAX_FILE_SIZE) {
            log.info("{} image file size {} exceeds maximum allowed size of 5 MB", type, file.getSize());
            errorDetailsList.add(new ErrorDetail("imageSize","Image file exceeds maximum size of 5 MB"));
            throw new PawnBrokingException(type.name() + "Image file exceeds maximum size of 5 MB", errorDetailsList);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.info("{} image file type {} is not allowed. Allowed types: JPEG, PNG, WebP", type, contentType);
            errorDetailsList.add(new ErrorDetail("imageContentType","Invalid image type. Allowed: JPEG, PNG, WebP"));
            throw new PawnBrokingException(type.name() + "Invalid image type. Allowed: JPEG, PNG, WebP", errorDetailsList);
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
