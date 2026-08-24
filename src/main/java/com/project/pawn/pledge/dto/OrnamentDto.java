package com.project.pawn.pledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrnamentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("default_interest_rate")
    private BigDecimal defaultInterestRate;

    @JsonProperty("default_amount_rate")
    private BigDecimal defaultAmountRate;

    @JsonProperty("is_active")
    private Boolean isActive;
}
