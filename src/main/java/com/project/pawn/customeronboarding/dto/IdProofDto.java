package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.common.enums.MediaType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class IdProofDto {
    @JsonProperty("id_proof_id")
    private Long idProofId;

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("id_type")
    @Enumerated(EnumType.STRING)
    private MediaType idType;

    @JsonProperty("id_number")
    private String idNumber;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image")
    private MultipartFile image;
}