package com.project.pawn.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemResponseDto {

    @JsonProperty("item_id")
    private Long itemId;

    //KEPT or RELEASED state
    @JsonProperty("action")
    private String action;

    @JsonProperty("amount")
    private BigDecimal amount;

    // --- Item creation fields (used in pledge bills) ---

    @JsonProperty("ornament_id")
    private Long ornamentId;

    @JsonProperty("description")
    private String description;

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

    @JsonProperty("item_images")
    private List<String> itemImage;
}
