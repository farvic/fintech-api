package com.farvic.fintech.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByFromAccountOrToAccount(Account fromAccount, Account toAccount, Pageable pageable);
}