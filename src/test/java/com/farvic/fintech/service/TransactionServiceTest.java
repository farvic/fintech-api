package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;

import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.entity.Account;
import com.farvic.fintech.entity.Transaction;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.exception.UnauthorizedOperationException;
import com.farvic.fintech.repository.AccountRepository;
import com.farvic.fintech.repository.TransactionRepository;
import com.farvic.fintech.repository.UserRepository;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private TransactionService transactionService;
    private Authentication authentication;

    private User user;
    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        authentication = mock(Authentication.class);

        transactionService = new TransactionService(transactionRepository, accountRepository, userRepository);

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
    }

    @Test
    void shouldTransferSuccessfully() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("150.00"),
                "Pagamento"
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = transactionService.transfer(request, authentication);

        assertEquals(new BigDecimal("350.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("250.00"), destinationAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), response.amount());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("999.00"),
                "Pagamento"
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));

        assertThrows(BusinessException.class, () -> transactionService.transfer(request, authentication));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldThrowWhenTransferingToSameAccount() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                sourceAccount.getId(),
                new BigDecimal("10.00"),
                "Pagamento"
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));

        assertThrows(BusinessException.class, () -> transactionService.transfer(request, authentication));
    }

    @Test
    void shouldThrowWhenSourceAccountDoesNotBelongToAuthenticatedUser() {
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Outro")
                .email("outro@email.com")
                .passwordHash("hash")
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        sourceAccount.setUser(anotherUser);

        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("50.00"),
                "Pagamento"
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));

        assertThrows(UnauthorizedOperationException.class, () -> transactionService.transfer(request, authentication));
    }
}