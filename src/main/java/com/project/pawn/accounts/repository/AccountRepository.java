package com.project.pawn.accounts.repository;

import com.project.pawn.accounts.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByIsActiveTrue();

    boolean existsByAccountNumber(String accountNumber);
}
