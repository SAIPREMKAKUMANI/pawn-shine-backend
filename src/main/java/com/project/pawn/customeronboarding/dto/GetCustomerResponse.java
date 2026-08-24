package com.project.pawn.customeronboarding.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class GetCustomerResponse {
    List<CustomerDto> customers;
}
