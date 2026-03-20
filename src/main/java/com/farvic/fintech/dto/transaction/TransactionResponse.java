package com.farvic.fintech.dto.transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.farvic.fintech.enums.TransactionStatus;
import com.farvic.fintech.enums.TransactionType;

public record TransactionResponse(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String description,
        Instant createdAt
) implements Serializable {
}