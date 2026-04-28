package com.project.pawn.accounts.service;

import com.project.pawn.accounts.dto.TransactionDto;
import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.mapper.AccountMapper;
import com.project.pawn.accounts.model.Account;
import com.project.pawn.accounts.model.Transaction;
import com.project.pawn.accounts.repository.AccountRepository;
import com.project.pawn.accounts.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final AccountMapper accountMapper;

    /**
     * Records a transaction and adjusts the account balance atomically.
     * This is the primary entry point for any money movement in the system.
     *
     * @param accountId       the account to transact on
     * @param amount          the amount
     * @param type            CREDIT (money in) or DEBIT (money out)
     * @param billId          associated bill ID (nullable for inter-account transfers)
     * @param description     human-readable description
     * @param referenceId     external reference (UPI ref, cheque #, etc.)
     * @return the recorded transaction DTO
     */
    @Transactional
    public TransactionDto recordTransaction(Long accountId, BigDecimal amount,
                                            TransactionType type, Long billId,
                                            String description, String referenceId) {
        log.info("Recording {} transaction of {} on account {}", type, amount, accountId);

        BigDecimal newBalance = accountService.adjustBalance(accountId, amount, type);

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .billId(billId)
                .transactionType(type.name())
                .amount(amount)
                .balanceAfter(newBalance)
                .transactionDate(LocalDateTime.now())
                .description(description)
                .referenceId(referenceId)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction recorded with ID: {}", saved.getId());

        TransactionDto dto = accountMapper.toTransactionDto(saved);
        enrichWithAccountNumber(dto, accountId);
        return dto;
    }

    public Page<TransactionDto> getTransactionsByAccount(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId, pageable)
                .map(t -> {
                    TransactionDto dto = accountMapper.toTransactionDto(t);
                    enrichWithAccountNumber(dto, t.getAccountId());
                    return dto;
                });
    }

    public List<TransactionDto> getTransactionsByBill(Long billId) {
        List<Transaction> transactions = transactionRepository.findByBillId(billId);
        return transactions.stream()
                .map(t -> {
                    TransactionDto dto = accountMapper.toTransactionDto(t);
                    enrichWithAccountNumber(dto, t.getAccountId());
                    return dto;
                })
                .toList();
    }

    public List<TransactionDto> getTransactionsByDateRange(Long accountId,
                                                           LocalDateTime from,
                                                           LocalDateTime to) {
        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(accountId, from, to);
        return transactions.stream()
                .map(t -> {
                    TransactionDto dto = accountMapper.toTransactionDto(t);
                    enrichWithAccountNumber(dto, t.getAccountId());
                    return dto;
                })
                .toList();
    }

    public Page<TransactionDto> getAllTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findAllByOrderByTransactionDateDesc(pageable)
                .map(t -> {
                    TransactionDto dto = accountMapper.toTransactionDto(t);
                    enrichWithAccountNumber(dto, t.getAccountId());
                    return dto;
                });
    }

    private void enrichWithAccountNumber(TransactionDto dto, Long accountId) {
        accountRepository.findById(accountId)
                .map(Account::getAccountNumber)
                .ifPresent(dto::setAccountNumber);
    }
}
