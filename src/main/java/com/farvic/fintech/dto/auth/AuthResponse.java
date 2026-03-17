package com.farvic.fintech.dto.auth;

public record AuthResponse(
        String token,
        String type
) {}