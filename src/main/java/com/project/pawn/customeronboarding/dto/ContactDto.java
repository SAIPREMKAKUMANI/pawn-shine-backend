package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactDto {

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("secondary_phone")
    private String secondaryPhone;

    @JsonProperty("whatsapp_phone")
    private String whatsappPhone;

    @JsonProperty("email")
    private String email;
}
