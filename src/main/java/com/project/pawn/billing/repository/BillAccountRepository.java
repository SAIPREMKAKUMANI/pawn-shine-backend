package com.project.pawn.billing.repository;

import com.project.pawn.billing.model.BillAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillAccountRepository extends JpaRepository<BillAccount, Long> {
}
