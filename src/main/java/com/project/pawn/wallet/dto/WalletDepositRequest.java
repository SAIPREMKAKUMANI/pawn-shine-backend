package com.project.pawn.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.billing.dto.BillAccountDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDepositRequest {
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("accounts")
    private List<BillAccountDto> accounts;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("transaction_date")
    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();
}

//We also need to add the used money from wallet, into bill's transaction list.

//When there multiple transactions or additions into wallet, we also need to include the transactions(with date), so customer will know that this money used from wallet.