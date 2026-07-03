package com.project.pawn.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request body for recording owner-decided interest on an item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordInterestRequestDto {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("interest_amount")
    private BigDecimal interestAmount;
}
