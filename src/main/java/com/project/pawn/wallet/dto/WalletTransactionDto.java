package com.project.pawn.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.wallet.enums.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("wallet_id")
    private Long walletId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("type")
    private WalletTransactionType type;

    @JsonProperty("transaction_date")
    private LocalDateTime transactionDate;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
