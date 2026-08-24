package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ContactDto {

    @JsonProperty("contact_id")
    private Long contactId;

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
