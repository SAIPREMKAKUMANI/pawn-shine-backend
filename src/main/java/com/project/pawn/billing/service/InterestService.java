package com.project.pawn.billing.service;

import com.project.pawn.billing.dto.InterestLedgerDto;
import com.project.pawn.billing.mapper.BillingMapper;
import com.project.pawn.billing.model.InterestLedger;
import com.project.pawn.billing.repository.InterestLedgerRepository;
import com.project.pawn.pledge.model.Item;
import com.project.pawn.pledge.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * InterestService — Owner-driven interest management.
 *
 * Interest is NOT auto-calculated. The owner decides the interest amount
 * based on their personal formulation strategy and records it manually.
 * This service provides the mechanism to record, update, and query
 * interest entries for audit purposes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

    private final InterestLedgerRepository interestLedgerRepository;
    private final ItemService itemService;
    private final BillingMapper billingMapper;

    /**
     * Records an owner-specified interest entry for an item.
     * The owner provides the exact interest amount — no formula is applied.
     *
     * @param itemId          the pledged item
     * @param interestAmount  the interest amount decided by the owner
     * @param notes           optional reason/notes for this interest entry
     * @return the recorded ledger entry
     */
    @Transactional
    public InterestLedgerDto recordInterest(Long itemId, BigDecimal interestAmount) {
        Item item = itemService.findItemOrThrow(itemId);

        BigDecimal previousCumulative = item.getCompoundInterest();
        BigDecimal newCumulative = previousCumulative.add(interestAmount);
        BigDecimal outstandingBalance = item.getAmountLended()
                .add(newCumulative)
                .subtract(item.getPaidAmount());

        InterestLedger entry = InterestLedger.builder()
                .itemId(itemId)
                .ledgerDate(LocalDate.now())
                .principal(item.getAmountLended())
                .interestAmount(interestAmount)
                .cumulativeInterest(newCumulative)
                .outstandingBalance(outstandingBalance)
                .build();

        interestLedgerRepository.save(entry);

        // Update item's compound_interest to reflect the new total
        itemService.updateInterest(itemId, newCumulative);

        log.info("Interest recorded for item {}: amount={}, cumulative={}, outstanding={}",
                itemId, interestAmount, newCumulative, outstandingBalance);

        return billingMapper.toInterestLedgerDto(entry);
    }

    /**
     * Allows the owner to override/correct the total interest on an item.
     * Replaces the cumulative interest entirely (not additive).
     *
     * Use case: Owner realizes they set wrong interest and wants to fix it.
     */
    @Transactional
    public InterestLedgerDto setTotalInterest(Long itemId, BigDecimal totalInterest) {
        Item item = itemService.findItemOrThrow(itemId);

        BigDecimal previousCumulative = item.getCompoundInterest();
        BigDecimal difference = totalInterest.subtract(previousCumulative);
        BigDecimal outstandingBalance = item.getAmountLended()
                .add(totalInterest)
                .subtract(item.getPaidAmount());

        InterestLedger entry = InterestLedger.builder()
                .itemId(itemId)
                .ledgerDate(LocalDate.now())
                .principal(item.getAmountLended())
                .interestAmount(difference)
                .cumulativeInterest(totalInterest)
                .outstandingBalance(outstandingBalance)
                .build();

        interestLedgerRepository.save(entry);
        itemService.updateInterest(itemId, totalInterest);

        log.info("Total interest overridden for item {}: new total={}, outstanding={}",
                itemId, totalInterest, outstandingBalance);

        return billingMapper.toInterestLedgerDto(entry);
    }

    public List<InterestLedgerDto> getInterestHistory(Long itemId) {
        List<InterestLedger> entries = interestLedgerRepository.findByItemIdOrderByLedgerDateDesc(itemId);
        return billingMapper.toInterestLedgerDtoList(entries);
    }

    public List<InterestLedgerDto> getInterestHistoryByDateRange(Long itemId, LocalDate from, LocalDate to) {
        List<InterestLedger> entries = interestLedgerRepository.findByItemIdAndDateRange(itemId, from, to);
        return billingMapper.toInterestLedgerDtoList(entries);
    }

    public InterestLedgerDto getCurrentInterestState(Long itemId) {
        return interestLedgerRepository.findTopByItemIdOrderByLedgerDateDesc(itemId)
                .map(billingMapper::toInterestLedgerDto)
                .orElse(null);
    }
}
