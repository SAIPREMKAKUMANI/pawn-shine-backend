package com.project.pawn.wallet.repository;

import com.project.pawn.wallet.model.WalletDepositAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletDepositAllocationRepository extends JpaRepository<WalletDepositAllocation, Long> {
    List<WalletDepositAllocation> findByWithdrawalTxId(Long withdrawalTxId);
    List<WalletDepositAllocation> findByBillId(Long billId);
}
