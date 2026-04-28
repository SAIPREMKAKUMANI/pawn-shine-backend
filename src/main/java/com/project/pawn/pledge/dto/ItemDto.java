package com.project.pawn.pledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.pledge.enums.ItemStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("ornament_id")
    private Long ornamentId;

    @JsonProperty("ornament_type")
    private String ornamentType;

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("weight_gross")
    private BigDecimal weightGross;

    @JsonProperty("weight_net")
    private BigDecimal weightNet;

    @JsonProperty("amount_lended")
    private BigDecimal amountLended;

    @JsonProperty("interest_rate")
    private BigDecimal interestRate;

    @JsonProperty("paid_amount")
    private BigDecimal paidAmount;

    @JsonProperty("compound_interest")
    private BigDecimal compoundInterest;

    @JsonProperty("outstanding_balance")
    private BigDecimal outstandingBalance;

    @JsonProperty("status")
    private ItemStatus status;

    @JsonProperty("location")
    private String location;

    @JsonProperty("pledge_date")
    private LocalDate pledgeDate;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("grace_period_days")
    private Integer gracePeriodDays;

    @JsonProperty("redeemed_date")
    private LocalDate redeemedDate;

    @JsonProperty("defaulted_date")
    private LocalDate defaultedDate;

    @JsonProperty("auctioned_date")
    private LocalDate auctionedDate;

    @JsonProperty("auction_amount")
    private BigDecimal auctionAmount;
}
