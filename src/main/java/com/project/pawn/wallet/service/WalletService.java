package com.project.pawn.wallet.service;

import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.service.TransactionService;
import com.project.pawn.billing.dto.BillAccountDto;
import com.project.pawn.customeronboarding.repository.CustomerRepository;
import com.project.pawn.wallet.dto.CustomerWalletDto;
import com.project.pawn.wallet.dto.WalletDepositRequest;
import com.project.pawn.wallet.dto.WalletTransactionDto;
import com.project.pawn.wallet.enums.WalletTransactionType;
import com.project.pawn.wallet.mapper.WalletMapper;
import com.project.pawn.wallet.model.CustomerWallet;
import com.project.pawn.wallet.model.WalletTransaction;
import com.project.pawn.wallet.repository.CustomerWalletRepository;
import com.project.pawn.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final CustomerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
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

    @Transactional
    public void withdraw(Long custId, BigDecimal amount, String referenceId, String notes) {
        log.info("Withdrawing {} from wallet for customer {}, ref: {}", amount, custId, referenceId);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        CustomerWallet wallet = getOrCreateWallet(custId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance. Available: " + wallet.getBalance() + ", Requested: " + amount);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .amount(amount)
                .type(WalletTransactionType.WITHDRAWAL)
                .referenceId(referenceId)
                .notes(notes)
                .transactionDate(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);
    }
}
