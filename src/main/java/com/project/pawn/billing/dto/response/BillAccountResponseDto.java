package com.project.pawn.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillAccountResponseDto {

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("direction")
    private String direction;
}
