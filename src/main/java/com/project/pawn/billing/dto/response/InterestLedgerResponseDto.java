package com.project.pawn.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestLedgerResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("ledger_date")
    private LocalDate ledgerDate;

    @JsonProperty("principal")
    private BigDecimal principal;

    @JsonProperty("interest_amount")
    private BigDecimal interestAmount;

    @JsonProperty("cumulative_interest")
    private BigDecimal cumulativeInterest;

    @JsonProperty("outstanding_balance")
    private BigDecimal outstandingBalance;
}
