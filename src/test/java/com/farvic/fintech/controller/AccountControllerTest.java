package com.farvic.fintech.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farvic.fintech.dto.account.AccountResponse;
import com.farvic.fintech.dto.account.DepositRequest;
import com.farvic.fintech.dto.account.WithdrawRequest;
import com.farvic.fintech.enums.AccountStatus;
import com.farvic.fintech.exception.GlobalExceptionHandler;
import com.farvic.fintech.security.JwtAuthenticationFilter;
import com.farvic.fintech.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Authentication auth() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "victor@email.com",
                null,
                List.of()
        );
    }

    @Test
    @DisplayName("POST /accounts deve criar conta")
    void shouldCreateAccountSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();

        AccountResponse response = new AccountResponse(
                id,
                "12345678",
                new BigDecimal("0.00"),
                AccountStatus.ACTIVE,
                Instant.now()
        );

        when(accountService.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/accounts")
                        .with(csrf())
                        .with(authentication(auth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.accountNumber").value("12345678"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /accounts deve listar contas do usuário")
    void shouldListAccountsSuccessfully() throws Exception {
        AccountResponse account1 = new AccountResponse(
                UUID.randomUUID(), "12345678", new BigDecimal("500.00"), AccountStatus.ACTIVE, Instant.now()
        );
        AccountResponse account2 = new AccountResponse(
                UUID.randomUUID(), "87654321", new BigDecimal("100.00"), AccountStatus.ACTIVE, Instant.now()
        );

        when(accountService.listMyAccounts(any()))
                .thenReturn(List.of(account1, account2));

        mockMvc.perform(get("/accounts")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("12345678"))
                .andExpect(jsonPath("$[1].accountNumber").value("87654321"));
    }

    @Test
    @DisplayName("GET /accounts/{id} deve retornar conta")
    void shouldGetAccountByIdSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();

        AccountResponse response = new AccountResponse(
                id,
                "12345678",
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE,
                Instant.now()
        );

        when(accountService.getMyAccountById(eq(id), any())).thenReturn(response);

        mockMvc.perform(get("/accounts/{id}", id)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.accountNumber").value("12345678"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposit deve depositar valor")
    void shouldDepositSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        DepositRequest request = new DepositRequest(new BigDecimal("200.00"));

        AccountResponse response = new AccountResponse(
                id,
                "12345678",
                new BigDecimal("700.00"),
                AccountStatus.ACTIVE,
                Instant.now()
        );

        when(accountService.deposit(eq(id), eq(request), any())).thenReturn(response);

        mockMvc.perform(post("/accounts/{id}/deposit", id)
                        .with(csrf())
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdraw deve sacar valor")
    void shouldWithdrawSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("100.00"));

        AccountResponse response = new AccountResponse(
                id,
                "12345678",
                new BigDecimal("400.00"),
                AccountStatus.ACTIVE,
                Instant.now()
        );

        when(accountService.withdraw(eq(id), eq(request), any())).thenReturn(response);

        mockMvc.perform(post("/accounts/{id}/withdraw", id)
                        .with(csrf())
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(400.00));
    }
}