package com.project.pawn.accounts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.accounts.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bill_id")
    private Long billId;

    @JsonProperty("transaction_type")
    private TransactionType transactionType;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("balance_after")
    private BigDecimal balanceAfter;

    @JsonProperty("transaction_date")
    private LocalDateTime transactionDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("reference_id")
    private String referenceId;
}
