package com.project.pawn.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single allocation from a wallet deposit to a bill.
 * Shows which deposit was consumed, how much, and whether it covered principal or interest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAllocationDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("deposit_transaction_id")
    private Long depositTransactionId;

    @JsonProperty("amount_used")
    private BigDecimal amountUsed;

    @JsonProperty("allocation_type")
    private String allocationType;

    @JsonProperty("deposit_date")
    private LocalDateTime depositDate;

    @JsonProperty("deposit_notes")
    private String depositNotes;
}
