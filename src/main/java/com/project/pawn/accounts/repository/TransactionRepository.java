package com.project.pawn.accounts.repository;

import com.project.pawn.accounts.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId, Pageable pageable);

    List<Transaction> findByBillId(Long billId);

    List<Transaction> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long accountId, LocalDateTime from, LocalDateTime to);

    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);
}
