package com.project.pawn.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemDto {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("amount")
    private BigDecimal amount;

    // --- Item creation fields (used in pledge bills) ---

    @JsonProperty("ornament_id")
    private Long ornamentId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("weight_gross")
    private BigDecimal weightGross;

    @JsonProperty("weight_net")
    private BigDecimal weightNet;

    @JsonProperty("interest_rate")
    private BigDecimal interestRate;

    @JsonProperty("location")
    private String location;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("grace_period_days")
    private Integer gracePeriodDays;
}
