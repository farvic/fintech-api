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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;

import com.farvic.fintech.dto.transaction.TransactionResponse;
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
        private IdempotencyService idempotencyService;
    private TransactionService transactionService;
    private Authentication authentication;
        private static final String IDEMPOTENCY_KEY = "idem-key-123";

    private User user;
    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        idempotencyService = mock(IdempotencyService.class);
        authentication = mock(Authentication.class);

        transactionService = new TransactionService(transactionRepository, accountRepository, userRepository, idempotencyService);

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

        when(idempotencyService.tryGetStoredResponse(any(UUID.class), anyString(), any(), eq(TransactionResponse.class)))
                .thenReturn(Optional.empty());
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

        var response = transactionService.transfer(request, authentication, IDEMPOTENCY_KEY);

        assertEquals(new BigDecimal("350.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("250.00"), destinationAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), response.amount());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
        verify(idempotencyService).saveResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq("TRANSFER"),
                eq(201),
                any(TransactionResponse.class)
        );
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

                BusinessException thrown = assertThrows(BusinessException.class,
                        () -> transactionService.transfer(request, authentication, IDEMPOTENCY_KEY));
                assertEquals("Insufficient balance", thrown.getMessage());

        verify(transactionRepository, never()).save(any(Transaction.class));
                verify(idempotencyService, never()).saveResponse(any(UUID.class), anyString(), any(), anyString(), anyInt(), any());
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

                BusinessException thrown = assertThrows(BusinessException.class,
                        () -> transactionService.transfer(request, authentication, IDEMPOTENCY_KEY));
                assertEquals("Source and destination accounts must be different", thrown.getMessage());
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

        UnauthorizedOperationException thrown = assertThrows(UnauthorizedOperationException.class,
                () -> transactionService.transfer(request, authentication, IDEMPOTENCY_KEY));
        assertEquals("You can only transfer from your own account", thrown.getMessage());
    }

    @Test
    void shouldReturnStoredResponseWhenIdempotentDuplicateRequest() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("50.00"),
                "Pagamento"
        );

        TransactionResponse storedResponse = new TransactionResponse(
                UUID.randomUUID(),
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("50.00"),
                com.farvic.fintech.enums.TransactionType.TRANSFER,
                com.farvic.fintech.enums.TransactionStatus.COMPLETED,
                "Pagamento",
                Instant.now()
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(idempotencyService.tryGetStoredResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq(TransactionResponse.class)
        )).thenReturn(Optional.of(storedResponse));

        TransactionResponse response = transactionService.transfer(request, authentication, IDEMPOTENCY_KEY);

        assertEquals(storedResponse, response);
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(idempotencyService, never()).saveResponse(any(UUID.class), anyString(), any(), anyString(), anyInt(), any());
    }

    @Test
    void shouldReturnRecoveredStoredResponseWhenSaveResponseRaisesDataIntegrityViolation() {
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("50.00"),
                "Pagamento"
        );

        TransactionResponse recoveredResponse = new TransactionResponse(
                UUID.randomUUID(),
                sourceAccount.getId(),
                destinationAccount.getId(),
                new BigDecimal("50.00"),
                com.farvic.fintech.enums.TransactionType.TRANSFER,
                com.farvic.fintech.enums.TransactionStatus.COMPLETED,
                "Pagamento",
                Instant.now()
        );

        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new DataIntegrityViolationException("duplicate key")).when(idempotencyService).saveResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq("TRANSFER"),
                eq(201),
                any(TransactionResponse.class)
        );

        when(idempotencyService.tryGetStoredResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq(TransactionResponse.class)
        )).thenReturn(Optional.empty()).thenReturn(Optional.of(recoveredResponse));

        TransactionResponse response = transactionService.transfer(request, authentication, IDEMPOTENCY_KEY);

        assertEquals(recoveredResponse, response);
    }

    @Test
    void shouldRethrowDataIntegrityViolationWhenRecoveryHasNoStoredResponse() {
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
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataIntegrityViolationException expected = new DataIntegrityViolationException("duplicate key");

        doThrow(expected).when(idempotencyService).saveResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq("TRANSFER"),
                eq(201),
                any(TransactionResponse.class)
        );

        when(idempotencyService.tryGetStoredResponse(
                eq(user.getId()),
                eq(IDEMPOTENCY_KEY),
                eq(request),
                eq(TransactionResponse.class)
        )).thenReturn(Optional.empty()).thenReturn(Optional.empty());

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> transactionService.transfer(request, authentication, IDEMPOTENCY_KEY)
        );

        assertEquals(expected, thrown);
    }
}