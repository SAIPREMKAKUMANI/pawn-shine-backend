package com.project.pawn.customeronboarding.repository;

import com.project.pawn.customeronboarding.model.AddressInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<AddressInfo, Long> {
}