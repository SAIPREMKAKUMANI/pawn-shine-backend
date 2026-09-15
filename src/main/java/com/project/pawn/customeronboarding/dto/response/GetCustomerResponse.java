package com.project.pawn.customeronboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class GetCustomerResponse {
    List<GetAllCustomerBaseResponse> customers;
}
