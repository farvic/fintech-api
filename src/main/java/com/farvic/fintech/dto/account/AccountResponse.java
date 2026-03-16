package com.farvic.fintech.dto.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.farvic.fintech.enums.AccountStatus;

public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
}