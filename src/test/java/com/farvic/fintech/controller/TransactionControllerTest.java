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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import com.farvic.fintech.dto.transaction.TransactionResponse;
import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;
import com.farvic.fintech.exception.GlobalExceptionHandler;
import com.farvic.fintech.security.JwtAuthenticationFilter;
import com.farvic.fintech.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
    private TransactionService transactionService;

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
    @DisplayName("POST /transfers deve realizar transferência")
    void shouldTransferSuccessfully() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        TransferRequest request = new TransferRequest(
                fromId,
                toId,
                new BigDecimal("50.00"),
                "Transferência teste"
        );

        TransactionResponse response = new TransactionResponse(
                txId,
                fromId,
                toId,
                new BigDecimal("50.00"),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "Transferência teste",
                Instant.now()
        );

        when(transactionService.transfer(eq(request), any())).thenReturn(response);

        mockMvc.perform(post("/transfers")
                        .with(csrf())
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /transactions deve listar histórico paginado")
    void shouldListTransactionsSuccessfully() throws Exception {
        UUID accountId = UUID.randomUUID();

        TransactionResponse transaction1 = new TransactionResponse(
                UUID.randomUUID(),
                null,
                accountId,
                new BigDecimal("500.00"),
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "Deposit",
                Instant.now()
        );

        TransactionResponse transaction2 = new TransactionResponse(
                UUID.randomUUID(),
                accountId,
                null,
                new BigDecimal("100.00"),
                TransactionType.WITHDRAW,
                TransactionStatus.COMPLETED,
                "Withdraw",
                Instant.now()
        );

        var page = new PageImpl<>(
                List.of(transaction1, transaction2),
                PageRequest.of(0, 10),
                2
        );

        when(transactionService.listMyTransactions(eq(accountId), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/transactions")
                        .with(authentication(auth()))
                        .param("accountId", accountId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[1].type").value("WITHDRAW"));
    }
}