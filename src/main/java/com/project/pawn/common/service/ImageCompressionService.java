package com.project.pawn.common.service;

import com.project.pawn.common.enums.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ImageCompressionService {

    public byte[] compressImage(MultipartFile file, MediaType mediaType) throws IOException {
        if (file == null || file.isEmpty()) {
            return new byte[0];
        }

        int maxWidth = 800;
        int maxHeight = 800;
        float quality = 0.7f;

        switch (mediaType) {
            case PROFILE:
            case RELATIVE:
                break;
            case AADHAR:
            case PAN:
            case PASSPORT:
            case VOTER_ID:
            case DRIVING_LICENSE:
            case ID_PROOF:
                maxWidth = 1920;
                maxHeight = 1080;
                quality = 0.85f;
                break;
            case PLEDGE_ITEM_IMAGE:
            case REDEEM_ITEM_IMAGE:
                maxWidth = 1280;
                maxHeight = 960;
                quality = 0.75f;
                break;
        }

        log.info("Compressing image for type {} using maxWidth={}, maxHeight={}, quality={}", 
                mediaType, maxWidth, maxHeight, quality);

        try (InputStream is = file.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Thumbnails.of(is)
                    .size(maxWidth, maxHeight)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toOutputStream(baos);

            byte[] compressedBytes = baos.toByteArray();
            log.info("Original image size: {} bytes, compressed size: {} bytes", file.getSize(), compressedBytes.length);
            return compressedBytes;
        } catch (Exception e) {
            log.error("Failed to compress image of type {}: {}", mediaType, e.getMessage(), e);
            throw new IOException("Failed to compress image: " + e.getMessage(), e);
        }
    }
}
