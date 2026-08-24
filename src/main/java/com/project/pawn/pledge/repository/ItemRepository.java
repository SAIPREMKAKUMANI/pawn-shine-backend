package com.project.pawn.pledge.repository;

import com.project.pawn.pledge.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByCustIdAndStatus(Long custId, String status);

    List<Item> findByCustId(Long custId);

    Page<Item> findByStatus(String status, Pageable pageable);

    List<Item> findByStatusAndDueDateBefore(String status, LocalDate date);

    List<Item> findByStatusAndDueDateBeforeAndGracePeriodDaysIsNotNull(String status, LocalDate date);

    @Query("SELECT SUM(i.amountLended) FROM Item i WHERE i.status = :status")
    BigDecimal sumAmountLendedByStatus(@Param("status") String status);

    @Query("SELECT COUNT(i) FROM Item i WHERE i.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(i.amountLended + i.compoundInterest - i.paidAmount) FROM Item i WHERE i.status = 'ACTIVE'")
    BigDecimal totalOutstandingBalance();
}
