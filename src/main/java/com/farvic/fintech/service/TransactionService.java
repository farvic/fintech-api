package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.farvic.fintech.dto.transaction.PageResponse;
import com.farvic.fintech.dto.transaction.TransactionResponse;
import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.exception.ResourceNotFoundException;
import com.farvic.fintech.exception.UnauthorizedOperationException;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.TransactionRepository;
import com.farvic.fintech.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
        @Caching(evict = {
            @CacheEvict(cacheNames = "transactionsByAccount", allEntries = true),
            @CacheEvict(cacheNames = "accountsByUser", allEntries = true),
            @CacheEvict(cacheNames = "accountById", allEntries = true)
        })
    public TransactionResponse transfer(TransferRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account fromAccount = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account toAccount = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        validateTransferOwnership(user, fromAccount);
        validateTransferRules(fromAccount, toAccount, request.amount());

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.amount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description(request.description())
                .createdAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Cacheable(
    cacheNames = "transactionsByAccount",
    key = "#authentication.name + ':' + #accountId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
)
    public PageResponse<TransactionResponse> listMyTransactions(UUID accountId, Pageable pageable, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateTransferOwnership(user, account);

        Page<TransactionResponse> page = transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(account, account, pageable)
                .map(this::toResponse);

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private void validateTransferOwnership(User user, Account fromAccount) {
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("You can only transfer from your own account");
        }
    }

    private void validateTransferRules(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BusinessException("Source and destination accounts must be different");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be greater than zero");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE || toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Both accounts must be active");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : null,
                transaction.getToAccount() != null ? transaction.getToAccount().getId() : null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}