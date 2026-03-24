package com.farvic.fintech.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farvic.fintech.config.JsonConfig;
import com.farvic.fintech.dto.transaction.TransactionResponse;
import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.entity.IdempotencyRecord;
import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;
import com.farvic.fintech.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class IdempotencyServiceTest {

    @Test
    void shouldSerializeTransactionResponseWithInstantWhenSavingIdempotentResponse() throws Exception {
        IdempotencyRecordRepository idempotencyRecordRepository = mock(IdempotencyRecordRepository.class);
        ObjectMapper objectMapper = new JsonConfig().objectMapper();
        IdempotencyService idempotencyService = new IdempotencyService(idempotencyRecordRepository, objectMapper);

        UUID userId = UUID.randomUUID();
        String idempotencyKey = "idem-key-123";

        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                "Pagamento"
        );

        TransactionResponse response = new TransactionResponse(
                UUID.randomUUID(),
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "Pagamento",
                Instant.parse("2026-03-24T18:22:16Z")
        );

        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> idempotencyService.saveResponse(
                userId,
                idempotencyKey,
                request,
                "TRANSFER",
                201,
                response
        ));

        ArgumentCaptor<IdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(idempotencyRecordRepository).save(recordCaptor.capture());

        IdempotencyRecord savedRecord = recordCaptor.getValue();
        JsonNode responseBody = objectMapper.readTree(savedRecord.getResponseBody());

        assertTrue(responseBody.hasNonNull("createdAt"));
        assertEquals(response.id().toString(), responseBody.get("id").asText());
        assertEquals(201, savedRecord.getResponseStatus());
        assertEquals(idempotencyKey, savedRecord.getIdempotencyKey());
        assertEquals(userId, savedRecord.getUserId());
    }
}