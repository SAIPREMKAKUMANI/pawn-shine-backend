package com.project.pawn.billing.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemRequestDto {

    private Long itemId;

    //KEPT or RELEASED state
    private String action;

    private BigDecimal amount;

    // --- Item creation fields (used in pledge bills) ---

    private Long ornamentId;

    private String description;

    private BigDecimal weightGross;

    private BigDecimal weightNet;

    private BigDecimal interestRate;

    private String location;

    private LocalDate dueDate;

    private Integer gracePeriodDays;

    private List<MultipartFile> itemImage;
}
