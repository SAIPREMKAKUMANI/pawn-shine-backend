package com.project.pawn.billing.repository;

import com.project.pawn.billing.model.InterestLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterestLedgerRepository extends JpaRepository<InterestLedger, Long> {

    List<InterestLedger> findByItemIdOrderByLedgerDateDesc(Long itemId);

    Optional<InterestLedger> findTopByItemIdOrderByLedgerDateDesc(Long itemId);

    boolean existsByItemIdAndLedgerDate(Long itemId, LocalDate ledgerDate);

    @Query("SELECT il FROM InterestLedger il WHERE il.itemId = :itemId AND il.ledgerDate BETWEEN :from AND :to ORDER BY il.ledgerDate ASC")
    List<InterestLedger> findByItemIdAndDateRange(
            @Param("itemId") Long itemId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
