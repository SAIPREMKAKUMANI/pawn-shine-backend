package com.project.pawn.billing.mapper;

import com.project.pawn.billing.dto.response.BillResponseDto;
import com.project.pawn.billing.dto.response.InterestLedgerResponseDto;
import com.project.pawn.billing.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BillingMapper {

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "items", source = "billItems")
    @Mapping(target = "accounts", source = "billAccounts")
    BillResponseDto toBillDto(Bill entity);

    InterestLedgerResponseDto toInterestLedgerDto(InterestLedger entity);

    List<InterestLedgerResponseDto> toInterestLedgerDtoList(List<InterestLedger> entities);
}
