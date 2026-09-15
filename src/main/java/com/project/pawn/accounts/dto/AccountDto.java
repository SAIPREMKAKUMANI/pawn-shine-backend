package com.project.pawn.accounts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.accounts.enums.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("account_type")
    private AccountType accountType;

    @JsonProperty("balance")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("disbursed_amount")
    @Builder.Default
    private BigDecimal disbursedAmount = BigDecimal.ZERO;

    @JsonProperty("repaid_amount")
    @Builder.Default
    private BigDecimal repaidAmount = BigDecimal.ZERO;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
