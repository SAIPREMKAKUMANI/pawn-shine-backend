package com.project.pawn.wallet.service;

import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.service.TransactionService;
import com.project.pawn.billing.dto.BillAccountDto;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.wallet.dto.CustomerWalletDto;
import com.project.pawn.wallet.dto.WalletAllocationDto;
import com.project.pawn.wallet.dto.WalletDepositRequest;
import com.project.pawn.wallet.dto.WalletTransactionDto;
import com.project.pawn.wallet.enums.WalletTransactionType;
import com.project.pawn.wallet.mapper.WalletMapper;
import com.project.pawn.wallet.model.CustomerWallet;
import com.project.pawn.wallet.model.WalletDepositAllocation;
import com.project.pawn.wallet.model.WalletTransaction;
import com.project.pawn.wallet.repository.CustomerWalletRepository;
import com.project.pawn.wallet.repository.WalletDepositAllocationRepository;
import com.project.pawn.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final CustomerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletDepositAllocationRepository allocationRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService globalTransactionService;
    private final WalletMapper walletMapper;

    public CustomerWallet getOrCreateWallet(Long custId) {
        return walletRepository.findByCustId(custId)
                .orElseGet(() -> {
                    if (!customerRepository.existsById(custId)) {
                        throw new IllegalArgumentException("Customer not found with ID: " + custId);
                    }
                    CustomerWallet newWallet = CustomerWallet.builder()
                            .custId(custId)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    public CustomerWalletDto getWalletDto(Long custId) {
        return walletMapper.toDto(getOrCreateWallet(custId));
    }

    public List<WalletTransactionDto> getWalletTransactions(Long custId) {
        CustomerWallet wallet = getOrCreateWallet(custId);
        return transactionRepository.findByWalletIdOrderByTransactionDateDesc(wallet.getId())
                .stream()
                .map(walletMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerWalletDto deposit(Long custId, WalletDepositRequest request) {
        log.info("Depositing {} to wallet for customer {}", request.getAmount(), custId);
        
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        
        if (request.getAccounts() == null || request.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("At least one payment account must be provided");
        }

        BigDecimal totalAccountsAmount = request.getAccounts().stream()
                .map(BillAccountDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAccountsAmount.compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Sum of account amounts must equal the deposit amount");
        }

        CustomerWallet wallet = getOrCreateWallet(custId);
        
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        wallet = walletRepository.save(wallet);

        LocalDateTime txDate = request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now();

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .amount(request.getAmount())
                .type(WalletTransactionType.DEPOSIT)
                .transactionDate(txDate)
                .notes(request.getNotes())
                .remainingBalance(request.getAmount()) // Track remaining for FIFO/LIFO allocation
                .build();
        transactionRepository.save(tx);

        // Record money IN to the owner's accounts
        for (BillAccountDto acctReq : request.getAccounts()) {
            globalTransactionService.recordTransaction(
                    acctReq.getAccountId(),
                    acctReq.getAmount(),
                    TransactionType.CREDIT,
                    null,
                    "Wallet Deposit for Customer " + custId + (request.getNotes() != null ? " - " + request.getNotes() : ""),
                    "WALLET-DEP-" + tx.getId()
            );
        }

        return walletMapper.toDto(wallet);
    }

    /**
     * Withdraws from the wallet using two-phase allocation:
     * Phase 1 (FIFO): Cover principal from oldest deposits first
     * Phase 2 (LIFO): Cover interest from newest deposits first
     *
     * @param custId            Customer ID
     * @param principalAmount   Amount to cover principal (FIFO — oldest deposits first)
     * @param interestAmount    Amount to cover interest (LIFO — newest deposits first)
     * @param referenceId       Reference for the withdrawal transaction
     * @param notes             Notes for the withdrawal transaction
     * @return List of allocation records showing which deposits were consumed
     */
    @Transactional
    public List<WalletDepositAllocation> withdraw(Long custId, BigDecimal principalAmount,
                                                   BigDecimal interestAmount, String referenceId, String notes) {
        BigDecimal totalAmount = principalAmount.add(interestAmount);
        log.info("Withdrawing {} (principal={}, interest={}) from wallet for customer {}, ref: {}",
                totalAmount, principalAmount, interestAmount, custId, referenceId);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        CustomerWallet wallet = getOrCreateWallet(custId);

        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance. Available: "
                    + wallet.getBalance() + ", Requested: " + totalAmount);
        }

        // Deduct from wallet balance
        wallet.setBalance(wallet.getBalance().subtract(totalAmount));
        walletRepository.save(wallet);

        // Create the WITHDRAWAL transaction
        WalletTransaction withdrawalTx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .amount(totalAmount)
                .type(WalletTransactionType.WITHDRAWAL)
                .referenceId(referenceId)
                .notes(notes)
                .transactionDate(LocalDateTime.now())
                .build();
        withdrawalTx = transactionRepository.save(withdrawalTx);

        List<WalletDepositAllocation> allocations = new ArrayList<>();

        // Phase 1: Cover PRINCIPAL using FIFO (oldest deposits first)
        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<WalletDepositAllocation> principalAllocations = allocateFromDeposits(
                    wallet.getId(), withdrawalTx.getId(), principalAmount, "PRINCIPAL", true);
            allocations.addAll(principalAllocations);
        }

        // Phase 2: Cover INTEREST using LIFO (newest deposits first)
        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            List<WalletDepositAllocation> interestAllocations = allocateFromDeposits(
                    wallet.getId(), withdrawalTx.getId(), interestAmount, "INTEREST", false);
            allocations.addAll(interestAllocations);
        }

        return allocations;
    }

    /**
     * Allocates a specific amount from deposits, consuming either FIFO or LIFO order.
     *
     * @param walletId          The wallet to allocate from
     * @param withdrawalTxId    The withdrawal transaction ID
     * @param amount            Amount to allocate
     * @param allocationType    "PRINCIPAL" or "INTEREST"
     * @param fifo              true = oldest first (FIFO), false = newest first (LIFO)
     * @return List of allocation records created
     */
    private List<WalletDepositAllocation> allocateFromDeposits(Long walletId, Long withdrawalTxId,
                                                               BigDecimal amount, String allocationType, boolean fifo) {
        List<WalletTransaction> deposits;
        if (fifo) {
            deposits = transactionRepository
                    .findByWalletIdAndTypeAndRemainingBalanceGreaterThanOrderByTransactionDateAsc(
                            walletId, WalletTransactionType.DEPOSIT, BigDecimal.ZERO);
        } else {
            deposits = transactionRepository
                    .findByWalletIdAndTypeAndRemainingBalanceGreaterThanOrderByTransactionDateDesc(
                            walletId, WalletTransactionType.DEPOSIT, BigDecimal.ZERO);
        }

        List<WalletDepositAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = amount;

        for (WalletTransaction deposit : deposits) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = deposit.getRemainingBalance();
            BigDecimal consume = remaining.min(available);

            // Decrease remaining balance on the deposit
            deposit.setRemainingBalance(available.subtract(consume));
            transactionRepository.save(deposit);

            // Create allocation record
            WalletDepositAllocation allocation = WalletDepositAllocation.builder()
                    .withdrawalTxId(withdrawalTxId)
                    .depositTxId(deposit.getId())
                    .amountUsed(consume)
                    .allocationType(allocationType)
                    .build();
            allocation = allocationRepository.save(allocation);
            allocations.add(allocation);

            remaining = remaining.subtract(consume);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Could not fully allocate " + allocationType
                    + " amount. Remaining unallocated: " + remaining);
        }

        return allocations;
    }

    /**
     * Fetches allocation records for a bill and maps them to DTOs
     * with deposit date and notes for display on bills/receipts.
     */
    public List<WalletAllocationDto> getAllocationsForBill(Long billId) {
        List<WalletDepositAllocation> allocations = allocationRepository.findByBillId(billId);
        return allocations.stream().map(alloc -> {
            WalletTransaction deposit = transactionRepository.findById(alloc.getDepositTxId()).orElse(null);
            return WalletAllocationDto.builder()
                    .id(alloc.getId())
                    .depositTransactionId(alloc.getDepositTxId())
                    .amountUsed(alloc.getAmountUsed())
                    .allocationType(alloc.getAllocationType())
                    .depositDate(deposit != null ? deposit.getTransactionDate() : null)
                    .depositNotes(deposit != null ? deposit.getNotes() : null)
                    .build();
        }).collect(Collectors.toList());
    }
}
