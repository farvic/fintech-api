package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.farvic.fintech.config.CacheConfig;
import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.TransactionRepository;
import com.farvic.fintech.repository.UserRepository;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        CacheConfig.class,
        TransactionService.class
})
class TransactionServiceCacheIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private UserRepository userRepository;

    private Authentication authentication;
    private User user;
    private Account sourceAccount;
    private Account destinationAccount;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        pageable = PageRequest.of(0, 10);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Victor")
                .email("victor@email.com")
                .passwordHash("hash")
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        sourceAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber("12345678")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        destinationAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(User.builder()
                        .id(UUID.randomUUID())
                        .name("Rotciv")
                        .email("rotciv@email.com")
                        .passwordHash("hash")
                        .role(Role.USER)
                        .createdAt(Instant.now())
                        .build())
                .accountNumber("87654321")
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .fromAccount(sourceAccount)
                .toAccount(destinationAccount)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description("Payment")
                .createdAt(Instant.now())
                .build();

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc(sourceAccount, sourceAccount, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(transaction), pageable, 1));

        org.springframework.cache.Cache cache = cacheManager.getCache("transactionsByAccount");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldCacheTransactionsHitAfterFirstMiss() {
        var firstCall = transactionService.listMyTransactions(sourceAccount.getId(), pageable, authentication);
        var secondCall = transactionService.listMyTransactions(sourceAccount.getId(), pageable, authentication);

                assertEquals(1, firstCall.totalElements());
                assertEquals(1, secondCall.totalElements());
        verify(transactionRepository, times(1))
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(sourceAccount, sourceAccount, pageable);
    }

    @Test
    void shouldMissAgainAfterCacheEvictOnTransfer() {
        transactionService.listMyTransactions(sourceAccount.getId(), pageable, authentication);
        transactionService.listMyTransactions(sourceAccount.getId(), pageable, authentication);

        verify(transactionRepository, times(1))
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(sourceAccount, sourceAccount, pageable);

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("10.00"),
                "Transfer test"
        );

        transactionService.transfer(request, authentication);
        transactionService.listMyTransactions(sourceAccount.getId(), pageable, authentication);

        verify(transactionRepository, times(2))
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(sourceAccount, sourceAccount, pageable);
    }
}
