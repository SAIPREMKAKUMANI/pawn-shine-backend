package com.project.pawn.customeronboarding.repository;
import com.project.pawn.customeronboarding.model.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<ContactInfo, Long> {
}

