package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class RelativeDto {
    @JsonProperty("relative_id")
    private Long relativeId;

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image")
    private MultipartFile image;

    @JsonProperty("name")
    private String name;

    @JsonProperty("relationship")
    private String relationship;

    @JsonProperty("contact_number")
    private String contactNumber;
}
