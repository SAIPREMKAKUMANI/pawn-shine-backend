package com.project.pawn.wallet.repository;

import com.project.pawn.wallet.enums.WalletTransactionType;
import com.project.pawn.wallet.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByTransactionDateDesc(Long walletId);
    Page<WalletTransaction> findByWalletIdOrderByTransactionDateDesc(Long walletId, Pageable pageable);

    // FIFO: oldest deposits with remaining balance (for principal coverage)
    List<WalletTransaction> findByWalletIdAndTypeAndRemainingBalanceGreaterThanOrderByTransactionDateAsc(
            Long walletId, WalletTransactionType type, BigDecimal minBalance);

    // LIFO: newest deposits with remaining balance (for interest coverage)
    List<WalletTransaction> findByWalletIdAndTypeAndRemainingBalanceGreaterThanOrderByTransactionDateDesc(
            Long walletId, WalletTransactionType type, BigDecimal minBalance);
}
