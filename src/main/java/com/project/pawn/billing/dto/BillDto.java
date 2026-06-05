package com.project.pawn.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.billing.enums.BillType;
import com.project.pawn.wallet.dto.WalletAllocationDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("bill_id")
    private String billId;

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("bill_type")
    private BillType billType;

    @JsonProperty("total_amount_lended")
    private BigDecimal totalAmountLended;

    @JsonProperty("amount_paid")
    private BigDecimal amountPaid;

    @JsonProperty("interest_accumulated")
    private BigDecimal interestAccumulated;

    @JsonProperty("bill_date")
    private LocalDate billDate;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("items")
    private List<BillItemDto> items;

    @JsonProperty("accounts")
    private List<BillAccountDto> accounts;

    @JsonProperty("wallet_allocations")
    private List<WalletAllocationDto> walletAllocations;

    @JsonProperty("wallet_amount_used")
    private BigDecimal walletAmountUsed;
}
