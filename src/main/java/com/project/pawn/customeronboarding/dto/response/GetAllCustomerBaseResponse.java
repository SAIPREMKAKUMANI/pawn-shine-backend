package com.project.pawn.customeronboarding.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class GetAllCustomerBaseResponse {
    @JsonProperty("cust_id")
    public Long custId;

    @JsonProperty("name")
    public String name;
}
