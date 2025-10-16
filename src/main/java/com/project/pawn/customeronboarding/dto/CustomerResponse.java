package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class CustomerResponse {
    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}

