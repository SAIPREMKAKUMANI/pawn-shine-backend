package com.project.pawn.accounts.repository;

import com.project.pawn.accounts.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByIsActiveTrue();

    boolean existsByAccountNumber(String accountNumber);
}
