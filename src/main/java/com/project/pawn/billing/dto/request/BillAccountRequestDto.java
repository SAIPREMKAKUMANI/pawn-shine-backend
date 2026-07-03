package com.project.pawn.billing.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillAccountRequestDto {

    private Long accountId;

    private String accountNumber;

    private BigDecimal amount;

    private String direction;
}
