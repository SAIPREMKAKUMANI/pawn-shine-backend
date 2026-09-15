package com.project.pawn.customeronboarding.service;

import com.project.pawn.common.service.ImageHandlingService;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.project.pawn.common.enums.MediaType.PROFILE;
import static com.project.pawn.common.enums.MediaType.RELATIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImageHandler {

    private final ImageHandlingService imageHandler;

    public void uploadAllImagesToDisk(CustomerDto request, Long custId) {
        log.info("Uploading images for customer: {}", request.getName());

        String profileImageUrl = imageHandler.uploadImageSafe(request.getImage(), custId, custId, PROFILE);
        if (profileImageUrl != null) {
            request.setImageUrl(profileImageUrl);
        }

        if (request.getIdProofs() != null) {
            request.getIdProofs().forEach(idProof -> {
                String proofImageUrl = imageHandler.uploadImageSafe(idProof.getImage(), custId, idProof.getIdProofId(), idProof.getIdType());
                if (proofImageUrl != null) {
                    idProof.setImageUrl(proofImageUrl);
                }
            });
        }

        if (request.getRelatives() != null) {
            request.getRelatives().forEach(relative -> {
                String relImageUrl = imageHandler.uploadImageSafe(relative.getImage(), custId, relative.getRelativeId(), RELATIVE);
                if (relImageUrl != null) {
                    relative.setImageUrl(relImageUrl);
                }
            });
        }

        log.info("All images uploaded successfully for customer: {}", request.getName());
    }
}
