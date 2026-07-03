package com.project.pawn.billing.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a new pledge bill (lending money, keeping items).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePledgeBillRequest {

    private Long custId;

    private List<BillItemRequestDto> items;

    private List<BillAccountRequestDto> accounts;

    private String notes;

    private LocalDate billDate;
}
