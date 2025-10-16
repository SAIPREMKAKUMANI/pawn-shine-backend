package com.project.pawn.customeronboarding.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.customeronboarding.model.AddressInfo;
import com.project.pawn.customeronboarding.model.ContactInfo;
import com.project.pawn.customeronboarding.model.IdProofInfo;
import com.project.pawn.customeronboarding.model.RelativeInfo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CustomerDto {
    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("nationality")
    private String nationality;

    @JsonProperty("marital_status")
    private String maritalStatus;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_by")
    private String updatedBy;

    @JsonProperty("contacts")
    private List<ContactDto> contacts;

    @JsonProperty("addresses")
    private List<AddressDto> addresses;

    @JsonProperty("id_proofs")
    private List<IdProofDto> idProofs;

    @JsonProperty("relatives")
    private List<RelativeDto> relatives;
}
