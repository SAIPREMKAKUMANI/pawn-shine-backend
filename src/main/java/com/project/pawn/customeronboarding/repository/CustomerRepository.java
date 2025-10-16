package com.project.pawn.customeronboarding.repository;

import com.project.pawn.customeronboarding.model.CustomerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerInfo, Long> {
    @Query("SELECT c.custId FROM CustomerInfo c JOIN c.idProofs i WHERE i.idNumber = :idNumber")
    boolean existsByIdProofs_IdNumber(String idNumber);

    @Query("SELECT c.custId FROM CustomerInfo c JOIN c.contacts con WHERE con.phone = :phone")
    boolean existsByContacts_WhatsappPhone(String phone);

    @Query("SELECT c.custId FROM CustomerInfo c JOIN c.contacts con WHERE con.whatsappPhone = :whatsappPhone")
    boolean existsByContacts_Phone(String whatsappPhone);

    @Query("SELECT c.custId FROM CustomerInfo c JOIN c.contacts con WHERE con.email = :email")
    boolean existsByContacts_Email(String email);
}
