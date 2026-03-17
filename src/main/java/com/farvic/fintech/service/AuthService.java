package com.farvic.fintech.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.farvic.fintech.dto.auth.AuthResponse;
import com.farvic.fintech.dto.auth.LoginRequest;
import com.farvic.fintech.dto.auth.RegisterRequest;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.repository.UserRepository;
import com.farvic.fintech.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer");
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer");
    }
}