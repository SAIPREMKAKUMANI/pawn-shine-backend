package com.project.pawn.customeronboarding.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.pawn.customeronboarding.enums.Gender;
import com.project.pawn.customeronboarding.enums.MaritalStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    @JsonProperty("cust_id")
    private Long custId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @JsonProperty("marital_status")
    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    @JsonProperty("status")
    private String status;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image")
    private MultipartFile image;

    @JsonProperty("contacts")
    private List<ContactDto> contacts;

    @JsonProperty("addresses")
    private List<AddressDto> addresses;

    @JsonProperty("id_proofs")
    private List<IdProofDto> idProofs;

    @JsonProperty("relatives")
    private List<RelativeDto> relatives;

    @JsonProperty("created_at")
    private LocalDate createdAt;

    @JsonProperty("updated_at")
    private LocalDate updatedAt;
}
