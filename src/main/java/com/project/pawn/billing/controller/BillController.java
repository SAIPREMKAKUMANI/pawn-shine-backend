package com.project.pawn.billing.controller;

import com.project.pawn.billing.dto.*;
import com.project.pawn.billing.enums.BillType;
import com.project.pawn.billing.service.BillService;
import com.project.pawn.billing.service.InterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final InterestService interestService;

    // =============================================
    // BILL CREATION
    // =============================================

    @PostMapping("/pledge")
    public ResponseEntity<BillDto> createPledgeBill(@RequestBody CreatePledgeBillRequest request) {
        BillDto bill = billService.createPledgeBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bill);
    }

    @PostMapping("/redeem")
    public ResponseEntity<BillDto> createRedemptionBill(@RequestBody CreateRedemptionBillRequest request) {
        BillDto bill = billService.createRedemptionBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bill);
    }

    // =============================================
    // BILL QUERIES
    // =============================================

    @GetMapping("/{id}")
    public ResponseEntity<BillDto> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillById(id));
    }

    @GetMapping("/ref/{billId}")
    public ResponseEntity<BillDto> getBillByBillId(@PathVariable String billId) {
        return ResponseEntity.ok(billService.getBillByBillId(billId));
    }

    @GetMapping("/customer/{custId}")
    public ResponseEntity<Page<BillDto>> getBillsByCustomer(
            @PathVariable Long custId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billService.getBillsByCustomer(custId, page, size));
    }

    @GetMapping
    public ResponseEntity<Page<BillDto>> getAllBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billService.getAllBills(page, size));
    }

    @GetMapping("/type/{billType}")
    public ResponseEntity<Page<BillDto>> getBillsByType(
            @PathVariable BillType billType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(billService.getBillsByType(billType, page, size));
    }

    // =============================================
    // INTEREST (Owner-set, not auto-calculated)
    // =============================================

    /**
     * Owner records an interest amount on an item (additive).
     * The amount is added to the item's existing compound_interest.
     */
    @PostMapping("/interest/record")
    public ResponseEntity<InterestLedgerDto> recordInterest(@RequestBody RecordInterestRequest request) {
        InterestLedgerDto entry = interestService.recordInterest(
                request.getItemId(), request.getInterestAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    /**
     * Owner overrides/corrects the total interest on an item (replaces).
     * Use when the owner made a mistake on a previous entry.
     */
    @PutMapping("/interest/set")
    public ResponseEntity<InterestLedgerDto> setTotalInterest(@RequestBody RecordInterestRequest request) {
        InterestLedgerDto entry = interestService.setTotalInterest(
                request.getItemId(), request.getInterestAmount());
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/interest/{itemId}")
    public ResponseEntity<List<InterestLedgerDto>> getInterestHistory(@PathVariable Long itemId) {
        return ResponseEntity.ok(interestService.getInterestHistory(itemId));
    }

    @GetMapping("/interest/{itemId}/range")
    public ResponseEntity<List<InterestLedgerDto>> getInterestHistoryByRange(
            @PathVariable Long itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(interestService.getInterestHistoryByDateRange(itemId, from, to));
    }

    @GetMapping("/interest/{itemId}/current")
    public ResponseEntity<InterestLedgerDto> getCurrentInterest(@PathVariable Long itemId) {
        InterestLedgerDto state = interestService.getCurrentInterestState(itemId);
        if (state == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(state);
    }
}
