package com.project.pawn.customeronboarding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class AddressDto {
    @JsonProperty("address_id")
    private Long addressId;

    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("street")
    private String street;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("postal_code")
    private String postalCode;

    @JsonProperty("country")
    private String country;
}