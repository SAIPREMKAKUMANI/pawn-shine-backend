package com.project.pawn.wallet.controller;

import com.project.pawn.common.service.RequestSanitizationService;
import com.project.pawn.wallet.dto.CustomerWalletDto;
import com.project.pawn.wallet.dto.WalletDepositRequest;
import com.project.pawn.wallet.dto.WalletTransactionDto;
import com.project.pawn.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final RequestSanitizationService sanitizationService;

    @GetMapping("/{custId}")
    public ResponseEntity<CustomerWalletDto> getWallet(@PathVariable Long custId) {
        return ResponseEntity.ok(walletService.getWalletDto(custId));
    }

    @GetMapping("/{custId}/transactions")
    public ResponseEntity<List<WalletTransactionDto>> getWalletTransactions(@PathVariable Long custId) {
        return ResponseEntity.ok(walletService.getWalletTransactions(custId));
    }

    @PostMapping("/{custId}/deposit")
    public ResponseEntity<CustomerWalletDto> deposit(@PathVariable Long custId, @RequestBody WalletDepositRequest request) {
        sanitizationService.sanitize(request);
        return new ResponseEntity<>(walletService.deposit(custId, request), HttpStatus.CREATED);
    }
}
