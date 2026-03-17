package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;

import com.farvic.fintech.dto.account.DepositRequest;
import com.farvic.fintech.dto.account.WithdrawRequest;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.exception.ResourceNotFoundException;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.TransactionRepository;
import com.farvic.fintech.repository.UserRepository;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    private Authentication authentication;

    private AccountService accountService;

    private User user;
    private Account account;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        authentication = mock(Authentication.class);

        accountService = new AccountService(accountRepository, transactionRepository, userRepository);

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
    }

    @Test
    void shouldCreateAccountSuccessfully() {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        var response = accountService.createAccount(authentication);

        assertNotNull(response);
        assertNotNull(response.id());
        assertNotNull(response.accountNumber());
        assertEquals(new BigDecimal("0.00"), response.balance());
        assertEquals(AccountStatus.ACTIVE, response.status());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        assertEquals(user, savedAccount.getUser());
        assertEquals(new BigDecimal("0.00"), savedAccount.getBalance());
        assertEquals(AccountStatus.ACTIVE, savedAccount.getStatus());
    }

    @Test
    void shouldListMyAccountsSuccessfully() {
        Account account2 = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber("87654321")
                .balance(new BigDecimal("150.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(List.of(account, account2));

        var response = accountService.listMyAccounts(authentication);

        assertEquals(2, response.size());
        assertEquals("12345678", response.get(0).accountNumber());
        assertEquals("87654321", response.get(1).accountNumber());
    }

    @Test
    void shouldGetMyAccountByIdSuccessfully() {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));

        var response = accountService.getMyAccountById(account.getId(), authentication);

        assertNotNull(response);
        assertEquals(account.getId(), response.id());
        assertEquals(account.getAccountNumber(), response.accountNumber());
        assertEquals(account.getBalance(), response.balance());
    }

    @Test
    void shouldThrowWhenGetMyAccountByIdAndAccountNotFound() {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getMyAccountById(account.getId(), authentication));
    }

    @Test
    void shouldDepositSuccessfully() {
        DepositRequest request = new DepositRequest(new BigDecimal("200.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.deposit(account.getId(), request, authentication);

        assertNotNull(response);
        assertEquals(new BigDecimal("700.00"), response.balance());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertNull(savedTransaction.getFromAccount());
        assertNotNull(savedTransaction.getToAccount());
        assertEquals(account.getId(), savedTransaction.getToAccount().getId());
        assertEquals(new BigDecimal("200.00"), savedTransaction.getAmount());
        assertEquals(TransactionType.DEPOSIT, savedTransaction.getType());
        assertEquals(TransactionStatus.COMPLETED, savedTransaction.getStatus());
    }

    @Test
    void shouldThrowWhenDepositIntoInactiveAccount() {
        account.setStatus(AccountStatus.BLOCKED);
        DepositRequest request = new DepositRequest(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> accountService.deposit(account.getId(), request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenDepositAccountNotFound() {
        DepositRequest request = new DepositRequest(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.deposit(account.getId(), request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldWithdrawSuccessfully() {
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("100.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.withdraw(account.getId(), request, authentication);

        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), response.balance());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertNotNull(savedTransaction.getFromAccount());
        assertEquals(account.getId(), savedTransaction.getFromAccount().getId());
        assertNull(savedTransaction.getToAccount());
        assertEquals(new BigDecimal("100.00"), savedTransaction.getAmount());
        assertEquals(TransactionType.WITHDRAW, savedTransaction.getType());
        assertEquals(TransactionStatus.COMPLETED, savedTransaction.getStatus());
    }

    @Test
    void shouldThrowWhenWithdrawFromInactiveAccount() {
        account.setStatus(AccountStatus.BLOCKED);
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("50.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> accountService.withdraw(account.getId(), request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenWithdrawInsufficientBalance() {
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("999.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> accountService.withdraw(account.getId(), request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenWithdrawAccountNotFound() {
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("50.00"));

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.withdraw(account.getId(), request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenAuthenticatedUserNotFoundOnCreateAccount() {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.createAccount(authentication));
    }

    @Test
    void shouldThrowWhenAuthenticatedUserNotFoundOnListAccounts() {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.listMyAccounts(authentication));
    }
}