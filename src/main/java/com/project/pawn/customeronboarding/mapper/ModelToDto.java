package com.project.pawn.customeronboarding.mapper;

import com.project.pawn.customeronboarding.dto.AddressDto;
import com.project.pawn.customeronboarding.dto.ContactDto;
import com.project.pawn.customeronboarding.dto.CustomerDto;
import com.project.pawn.customeronboarding.dto.IdProofDto;
import com.project.pawn.customeronboarding.dto.RelativeDto;
import com.project.pawn.customeronboarding.model.AddressInfo;
import com.project.pawn.customeronboarding.model.ContactInfo;
import com.project.pawn.customeronboarding.model.CustomerInfo;
import com.project.pawn.customeronboarding.model.IdProofInfo;
import com.project.pawn.customeronboarding.model.RelativeInfo;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ModelToDto {

    @Mapping(source = "zipCode", target = "postalCode")
    AddressDto toAddressDto(AddressInfo source);

    ContactDto toContactDto(ContactInfo source);

    RelativeDto toRelativeDto(RelativeInfo source);

    @Mapping(
            target = "idType",
            expression = "java(source.getIdType() != null ? com.project.pawn.customeronboarding.enums.MediaType.valueOf(source.getIdType()) : null)"
    )
    IdProofDto toIdProofDto(IdProofInfo source);

    // List mappings with null filtering
    @Named("toAddressDtos")
    default List<AddressDto> toAddressDtos(List<AddressInfo> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::toAddressDto)
                .collect(Collectors.toList());
    }

    @Named("toContactDtos")
    default List<ContactDto> toContactDtos(List<ContactInfo> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::toContactDto)
                .collect(Collectors.toList());
    }

    @Named("toRelativeDtos")
    default List<RelativeDto> toRelativeDtos(List<RelativeInfo> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::toRelativeDto)
                .collect(Collectors.toList());
    }

    @Named("toIdProofDtos")
    default List<IdProofDto> toIdProofDtos(List<IdProofInfo> list) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::toIdProofDto)
                .collect(Collectors.toList());
    }

    // Full customer DTO mapping with all nested collections and enum conversions
    @Mapping(
            target = "gender",
            expression = "java(source.getGender() != null ? com.project.pawn.customeronboarding.enums.Gender.valueOf(source.getGender()) : null)"
    )
    @Mapping(
            target = "maritalStatus",
            expression = "java(source.getMaritalStatus() != null ? com.project.pawn.customeronboarding.enums.MaritalStatus.valueOf(source.getMaritalStatus()) : null)"
    )
    @Mapping(target = "contacts", source = "contacts", qualifiedByName = "toContactDtos")
    @Mapping(target = "addresses", source = "addresses", qualifiedByName = "toAddressDtos")
    @Mapping(target = "relatives", source = "relatives", qualifiedByName = "toRelativeDtos")
    @Mapping(target = "idProofs", source = "idProofs", qualifiedByName = "toIdProofDtos")
    CustomerDto mapToFullCustomerDTO(CustomerInfo source);
}