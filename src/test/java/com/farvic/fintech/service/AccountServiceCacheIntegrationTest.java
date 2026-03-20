package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.farvic.fintech.config.CacheConfig;
import com.farvic.fintech.dto.account.DepositRequest;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.TransactionRepository;
import com.farvic.fintech.repository.UserRepository;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        CacheConfig.class,
        AccountService.class
})
class AccountServiceCacheIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    private Authentication authentication;
    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Victor Araujo")
                .email("victor@email.com")
                .passwordHash("hash")
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        account = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber("12345678")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(List.of(account));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        org.springframework.cache.Cache cache = cacheManager.getCache("accountsByUser");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldCacheListMyAccountsHitAfterFirstMiss() {
        var firstCall = accountService.listMyAccounts(authentication);
        var secondCall = accountService.listMyAccounts(authentication);

        assertEquals(1, firstCall.size());
        assertEquals(1, secondCall.size());
        verify(accountRepository, times(1)).findByUser(user);
    }

    @Test
    void shouldMissAgainAfterCacheEvictOnDeposit() {
        accountService.listMyAccounts(authentication);
        accountService.listMyAccounts(authentication);

        verify(accountRepository, times(1)).findByUser(user);

        accountService.deposit(account.getId(), new DepositRequest(new BigDecimal("10.00")), authentication);
        accountService.listMyAccounts(authentication);

        verify(accountRepository, times(2)).findByUser(user);
    }
}
