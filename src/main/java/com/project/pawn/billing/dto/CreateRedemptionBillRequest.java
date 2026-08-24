package com.project.pawn.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a redemption bill (customer paying back, releasing items).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRedemptionBillRequest {

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("item_ids")
    private List<Long> itemIds;

    @JsonProperty("wallet_amount_used")
    private BigDecimal walletAmountUsed;

    @JsonProperty("accounts")
    private List<BillAccountDto> accounts;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("bill_date")
    private LocalDate billDate;
}
