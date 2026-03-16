package com.farvic.fintech.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.User;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUser(User user);
    Optional<Account> findByIdAndUser(UUID id, User user);
    boolean existsByAccountNumber(String accountNumber);
}