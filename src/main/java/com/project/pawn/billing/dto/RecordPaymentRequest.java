package com.project.pawn.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordPaymentRequest {

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;

    @JsonProperty("wallet_amount_used")
    private BigDecimal walletAmountUsed;

    @JsonProperty("accounts")
    private List<BillAccountDto> accounts;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("bill_date")
    private LocalDate billDate;
}
