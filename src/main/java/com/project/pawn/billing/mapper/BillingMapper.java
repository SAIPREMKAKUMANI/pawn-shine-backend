package com.project.pawn.billing.mapper;

import com.project.pawn.billing.dto.*;
import com.project.pawn.billing.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BillingMapper {

    @Mapping(target = "billType",
            expression = "java(entity.getBillType() != null ? com.project.pawn.billing.enums.BillType.valueOf(entity.getBillType()) : null)")
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "items", source = "billItems")
    @Mapping(target = "accounts", source = "billAccounts")
    BillDto toBillDto(Bill entity);

    List<BillDto> toBillDtoList(List<Bill> entities);

    @Mapping(target = "ornamentId", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "weightGross", ignore = true)
    @Mapping(target = "weightNet", ignore = true)
    @Mapping(target = "interestRate", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "gracePeriodDays", ignore = true)
    BillItemDto toBillItemDto(BillItem entity);

    @Mapping(target = "accountNumber", ignore = true)
    BillAccountDto toBillAccountDto(BillAccount entity);

    InterestLedgerDto toInterestLedgerDto(InterestLedger entity);

    List<InterestLedgerDto> toInterestLedgerDtoList(List<InterestLedger> entities);
}
