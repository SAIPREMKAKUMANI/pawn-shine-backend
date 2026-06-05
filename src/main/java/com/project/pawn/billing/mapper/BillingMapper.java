package com.project.pawn.billing.mapper;

import com.project.pawn.billing.dto.*;
import com.project.pawn.billing.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BillingMapper {

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "items", source = "billItems")
    @Mapping(target = "accounts", source = "billAccounts")
    BillDto toBillDto(Bill entity);

    InterestLedgerDto toInterestLedgerDto(InterestLedger entity);

    List<InterestLedgerDto> toInterestLedgerDtoList(List<InterestLedger> entities);
}
