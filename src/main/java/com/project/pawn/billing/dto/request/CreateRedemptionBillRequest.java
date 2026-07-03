package com.project.pawn.billing.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a redemption bill (customer paying back, releasing items).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRedemptionBillRequest {

    private Long custId;

    private List<Long> itemIds;

    private BigDecimal walletAmountUsed;

    private List<BillAccountRequestDto> accounts;

    private String notes;

    private LocalDate billDate;

    private List<MultipartFile> itemImages;
}