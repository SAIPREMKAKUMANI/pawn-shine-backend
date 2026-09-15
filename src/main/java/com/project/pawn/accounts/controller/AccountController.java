package com.project.pawn.accounts.controller;

import com.project.pawn.accounts.dto.AccountDto;
import com.project.pawn.accounts.dto.TransactionDto;
import com.project.pawn.accounts.service.AccountService;
import com.project.pawn.accounts.service.TransactionService;
import com.project.pawn.common.service.RequestSanitizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final RequestSanitizationService sanitizationService;

    // --- Account endpoints ---

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto request) {
        sanitizationService.sanitize(request);
        AccountDto created = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts(
            @RequestParam(value = "active_only", defaultValue = "true") boolean activeOnly) {
        List<AccountDto> accounts = activeOnly
                ? accountService.getAllActiveAccounts()
                : accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id, @RequestBody AccountDto request) {
        sanitizationService.sanitize(request);
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {
        accountService.deactivateAccount(id);
        return ResponseEntity.noContent().build();
    }

    // --- Transaction endpoints ---

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId, page, size));
    }

    @GetMapping("/{accountId}/transactions/range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDateRange(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(accountId, from, to));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size));
    }
}
