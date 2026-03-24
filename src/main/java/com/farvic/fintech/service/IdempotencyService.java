package com.farvic.fintech.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.farvic.fintech.entity.IdempotencyRecord;
import com.farvic.fintech.exception.IdempotencyConflictException;
import com.farvic.fintech.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> tryGetStoredResponse(
            UUID userId,
            String idempotencyKey,
            Object request,
            Class<T> responseType
    ) {
        Optional<IdempotencyRecord> existing =
                idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        String currentHash = hashRequest(request);
        IdempotencyRecord record = existing.get();

        if (!record.getRequestHash().equals(currentHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used with a different request payload"
            );
        }

        try {
            return Optional.of(objectMapper.readValue(record.getResponseBody(), responseType));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize stored idempotent response", e);
        }
    }

    public void saveResponse(
            UUID userId,
            String idempotencyKey,
            Object request,
            String resourceType,
            int responseStatus,
            Object responseBody
    ) {
        String requestHash = hashRequest(request);

        try {
            String serializedResponse = objectMapper.writeValueAsString(responseBody);

            IdempotencyRecord record = IdempotencyRecord.builder()
                    .userId(userId)
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .resourceType(resourceType)
                    .responseStatus(responseStatus)
                    .responseBody(serializedResponse)
                    .createdAt(Instant.now())
                    .build();

            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize idempotent response", e);
        }
    }

    public String hashRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash request", e);
        }
    }
}