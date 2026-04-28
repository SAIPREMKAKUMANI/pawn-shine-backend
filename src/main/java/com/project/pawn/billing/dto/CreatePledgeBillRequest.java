package com.project.pawn.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a new pledge bill (lending money, keeping items).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePledgeBillRequest {

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("items")
    private List<BillItemDto> items;

    @JsonProperty("accounts")
    private List<BillAccountDto> accounts;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("bill_date")
    private LocalDate billDate;
}
