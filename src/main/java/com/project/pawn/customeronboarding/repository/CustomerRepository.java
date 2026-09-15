package com.project.pawn.customeronboarding.repository;

import com.project.pawn.customeronboarding.model.CustomerInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerInfo, Long> {
    Optional<CustomerInfo> findByCustId(Long custId);
    Page<CustomerInfo> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT new com.project.pawn.customeronboarding.dto.response.GetAllCustomerBaseResponse(c.custId, c.name) FROM CustomerInfo c")
    List<com.project.pawn.customeronboarding.dto.response.GetAllCustomerBaseResponse> findAllBaseCustomers();
}
