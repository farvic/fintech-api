package com.farvic.fintech.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.farvic.fintech.dto.transaction.PageResponse;
import com.farvic.fintech.dto.transaction.TransactionResponse;
import com.farvic.fintech.dto.transaction.TransferRequest;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.http.IdempotencyHeaders;
import com.farvic.fintech.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(
        @RequestHeader(value = IdempotencyHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
        @Valid @RequestBody TransferRequest request,
        Authentication authentication
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(IdempotencyHeaders.IDEMPOTENCY_KEY + " header is required");
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 255) {
            throw new BusinessException(IdempotencyHeaders.IDEMPOTENCY_KEY + " must have at most 255 characters");
        }

        TransactionService.TransferResult result = transactionService.transfer(request, authentication, normalizedKey);

        if (result.replayed()) {
            return ResponseEntity.status(HttpStatus.OK)
                .header(IdempotencyHeaders.IDEMPOTENCY_REPLAYED, "true")
                .header(IdempotencyHeaders.IDEMPOTENCY_MESSAGE, "Transaction already processed for this Idempotency-Key")
                    .body(result.response());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .header(IdempotencyHeaders.IDEMPOTENCY_REPLAYED, "false")
                .body(result.response());
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> listMyTransactions(@RequestParam UUID accountId,
                                                        Pageable pageable,
                                                        Authentication authentication) {
        return transactionService.listMyTransactions(accountId, pageable, authentication);
    }
}