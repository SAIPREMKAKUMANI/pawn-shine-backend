package com.project.pawn.billing.repository;

import com.project.pawn.billing.enums.BillType;
import com.project.pawn.billing.model.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillId(String billId);

    List<Bill> findByCustId(Long custId);

    Page<Bill> findByCustIdOrderByBillDateDesc(Long custId, Pageable pageable);

    Page<Bill> findByBillTypeOrderByBillDateDesc(BillType billType, Pageable pageable);

    Page<Bill> findAllByOrderByBillDateDesc(Pageable pageable);

    List<Bill> findByBillDateBetween(LocalDate from, LocalDate to);

    boolean existsByBillId(String billId);
}
