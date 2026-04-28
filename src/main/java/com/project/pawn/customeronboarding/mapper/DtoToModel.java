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
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DtoToModel {

    default CustomerInfo toModel(CustomerDto dto) {
        if (dto == null) {
            return null;
        }
        CustomerInfo customerInfo = new CustomerInfo();
        return updateModel(dto, customerInfo, customerInfo);
    }

    @Mapping(target = "gender", expression = "java(dto.getGender() != null ? dto.getGender().name() : null)")
    @Mapping(target = "maritalStatus", expression = "java(dto.getMaritalStatus() != null ? dto.getMaritalStatus().name() : null)")
    @Mapping(target = "contacts", source = "contacts", qualifiedByName = "toContactModelList")
    @Mapping(target = "addresses", source = "addresses", qualifiedByName = "toAddressModelList")
    @Mapping(target = "relatives", source = "relatives", qualifiedByName = "toRelativeModelList")
    @Mapping(target = "idProofs", source = "idProofs", qualifiedByName = "toIdProofModelList")
    CustomerInfo updateModel(CustomerDto dto, @MappingTarget CustomerInfo target, @Context CustomerInfo context);

    @Named("toContactModelList")
    default List<ContactInfo> toContactModelList(List<ContactDto> contactDtos, @Context CustomerInfo context) {
        if (contactDtos == null) {
            return Collections.emptyList();
        }
        return contactDtos.stream()
                .map(contactDto -> {
                    ContactInfo contact = toModel(contactDto);
                    contact.setCustomer(context);
                    return contact;
                })
                .collect(Collectors.toList());
    }

    @Named("toAddressModelList")
    default List<AddressInfo> toAddressModelList(List<AddressDto> addressDtos, @Context CustomerInfo context) {
        if (addressDtos == null) {
            return Collections.emptyList();
        }
        return addressDtos.stream()
                .map(addressDto -> {
                    AddressInfo address = toModel(addressDto);
                    address.setCustomer(context);
                    return address;
                })
                .collect(Collectors.toList());
    }

    @Named("toRelativeModelList")
    default List<RelativeInfo> toRelativeModelList(List<RelativeDto> relativeDtos, @Context CustomerInfo context) {
        if (relativeDtos == null) {
            return Collections.emptyList();
        }
        return relativeDtos.stream()
                .map(relativeDto -> {
                    RelativeInfo relative = toModel(relativeDto);
                    relative.setCustomer(context);
                    return relative;
                })
                .collect(Collectors.toList());
    }

    @Named("toIdProofModelList")
    default List<IdProofInfo> toIdProofModelList(List<IdProofDto> idProofDtos, @Context CustomerInfo context) {
        if (idProofDtos == null) {
            return Collections.emptyList();
        }
        return idProofDtos.stream()
                .map(idProofDto -> {
                    IdProofInfo idProof = toModel(idProofDto);
                    idProof.setCustomer(context);
                    return idProof;
                })
                .collect(Collectors.toList());
    }

    ContactInfo toModel(ContactDto dto);

    @Mapping(source = "postalCode", target = "zipCode")
    AddressInfo toModel(AddressDto dto);

    RelativeInfo toModel(RelativeDto dto);

    @Mapping(target = "idType", expression = "java(dto.getIdType() != null ? dto.getIdType().name() : null)")
    IdProofInfo toModel(IdProofDto dto);
}