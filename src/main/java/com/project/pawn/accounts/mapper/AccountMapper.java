package com.project.pawn.accounts.mapper;

import com.project.pawn.accounts.dto.AccountDto;
import com.project.pawn.accounts.dto.TransactionDto;
import com.project.pawn.accounts.model.Account;
import com.project.pawn.accounts.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "accountType",
            expression = "java(entity.getAccountType() != null ? com.project.pawn.accounts.enums.AccountType.valueOf(entity.getAccountType()) : null)")
    AccountDto toDto(Account entity);

    List<AccountDto> toDtoList(List<Account> entities);

    @Mapping(target = "accountType",
            expression = "java(dto.getAccountType() != null ? dto.getAccountType().name() : null)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(AccountDto dto);

    @Mapping(target = "transactionType",
            expression = "java(entity.getTransactionType() != null ? com.project.pawn.accounts.enums.TransactionType.valueOf(entity.getTransactionType()) : null)")
    @Mapping(target = "accountNumber", ignore = true)
    TransactionDto toTransactionDto(Transaction entity);

    List<TransactionDto> toTransactionDtoList(List<Transaction> entities);
}
