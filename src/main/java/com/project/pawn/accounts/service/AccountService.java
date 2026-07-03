package com.project.pawn.accounts.service;

import com.project.pawn.accounts.dto.AccountDto;
import com.project.pawn.accounts.enums.TransactionType;
import com.project.pawn.accounts.mapper.AccountMapper;
import com.project.pawn.accounts.model.Account;
import com.project.pawn.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountDto createAccount(AccountDto request) {
        log.info("Creating account: {}", request.getAccountNumber());
        validateAccountNumber(request.getAccountNumber());

        Account account = accountMapper.toEntity(request);
        account.setBalance(BigDecimal.ZERO);
        account.setIsActive(true);

        Account saved = accountRepository.save(account);
        log.info("Account created with ID: {}", saved.getId());
        return accountMapper.toDto(saved);
    }

    public AccountDto getAccountById(Long id) {
        Account account = findAccountOrThrow(id);
        return accountMapper.toDto(account);
    }

    public List<AccountDto> getAllActiveAccounts() {
        return accountMapper.toDtoList(accountRepository.findByIsActiveTrue());
    }

    public List<AccountDto> getAllAccounts() {
        return accountMapper.toDtoList(accountRepository.findAll());
    }

    @Transactional
    public AccountDto updateAccount(Long id, AccountDto request) {
        Account account = findAccountOrThrow(id);

        if (request.getBankName() != null) {
            account.setBankName(request.getBankName());
        }
        if (request.getAccountType() != null) {
            account.setAccountType(request.getAccountType().name());
        }
        if (request.getIsActive() != null) {
            account.setIsActive(request.getIsActive());
        }

        Account saved = accountRepository.save(account);
        log.info("Account updated: {}", id);
        return accountMapper.toDto(saved);
    }

    @Transactional
    public void deactivateAccount(Long id) {
        Account account = findAccountOrThrow(id);
        account.setIsActive(false);
        accountRepository.save(account);
        log.info("Account deactivated: {}", id);
    }

    /**
     * Adjusts account balance and returns the new balance.
     * Called internally by TransactionService — not exposed via controller.
     * This is the single point of balance mutation.
     */
    @Transactional
    public BigDecimal adjustBalance(Long accountId, BigDecimal amount, TransactionType type) {
        Account account = findAccountOrThrow(accountId);
        BigDecimal totalDisbursed = account.getDisbursedAmount();
        BigDecimal totalRepaid = account.getRepaidAmount();

        if (TransactionType.CREDIT.equals(type)) {
            totalRepaid = totalRepaid.add(amount);
            account.setRepaidAmount(totalRepaid);
        } else if (TransactionType.DEBIT.equals(type)) {
            totalDisbursed = totalDisbursed.add(amount);
            account.setDisbursedAmount(totalDisbursed);
        }

        BigDecimal newBalance = switch (type) {
            case CREDIT -> account.getBalance().add(amount);
            case DEBIT -> account.getBalance().subtract(amount);
        };

        account.setBalance(newBalance);
        accountRepository.save(account);
        log.info("Account {} balance adjusted by {} ({}). New balance: {}",
                accountId, amount, type, newBalance);
        return newBalance;
    }

    // --- Private helpers ---

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
    }

    private void validateAccountNumber(String accountNumber) {
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new IllegalArgumentException("Account number already exists: " + accountNumber);
        }
    }
}
