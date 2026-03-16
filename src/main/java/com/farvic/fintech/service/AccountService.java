package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.farvic.fintech.dto.account.AccountResponse;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.exception.ResourceNotFoundException;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountResponse createAccount(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        account = accountRepository.save(account);
        return toResponse(account);
    }

    public List<AccountResponse> listMyAccounts(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return accountRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getMyAccountById(UUID accountId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return toResponse(account);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(ThreadLocalRandom.current().nextLong(10000000L, 99999999L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}