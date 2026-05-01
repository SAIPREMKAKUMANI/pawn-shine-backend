package com.project.pawn.wallet.mapper;

import com.project.pawn.wallet.dto.CustomerWalletDto;
import com.project.pawn.wallet.dto.WalletTransactionDto;
import com.project.pawn.wallet.model.CustomerWallet;
import com.project.pawn.wallet.model.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public CustomerWalletDto toDto(CustomerWallet entity) {
        if (entity == null) return null;
        return CustomerWalletDto.builder()
                .id(entity.getId())
                .custId(entity.getCustId())
                .balance(entity.getBalance())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public WalletTransactionDto toDto(WalletTransaction entity) {
        if (entity == null) return null;
        return WalletTransactionDto.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .amount(entity.getAmount())
                .type(entity.getType())
                .transactionDate(entity.getTransactionDate())
                .referenceId(entity.getReferenceId())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
