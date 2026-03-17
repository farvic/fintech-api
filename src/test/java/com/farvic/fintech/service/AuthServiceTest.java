package com.farvic.fintech.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.farvic.fintech.dto.auth.AuthResponse;
import com.farvic.fintech.dto.auth.LoginRequest;
import com.farvic.fintech.dto.auth.RegisterRequest;
import com.farvic.fintech.entity.User;
import com.farvic.fintech.enums.Role;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.repository.UserRepository;
import com.farvic.fintech.security.JwtService;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);

        authService = new AuthService(userRepository, passwordEncoder, jwtService);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Victor Araujo")
                .email("victor@email.com")
                .passwordHash("encoded-password")
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Victor Araujo",
                "victor@email.com",
                "12345678"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                        User saved = invocation.getArgument(0);
                        if (saved.getId() == null) {
                                saved.setId(UUID.randomUUID());
                        }
                        return saved;
                });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.type());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getId());
        assertEquals("Victor Araujo", savedUser.getName());
        assertEquals("victor@email.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals(Role.USER, savedUser.getRole());
        assertNotNull(savedUser.getCreatedAt());

        verify(passwordEncoder).encode("12345678");
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    void shouldThrowWhenRegisterWithDuplicatedEmail() {
        RegisterRequest request = new RegisterRequest(
                "Victor Araujo",
                "victor@email.com",
                "12345678"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest(
                "victor@email.com",
                "12345678"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.type());

        verify(userRepository).findByEmail("victor@email.com");
        verify(passwordEncoder).matches("12345678", "encoded-password");
        verify(jwtService).generateToken(user);
    }

    @Test
    void shouldThrowWhenLoginWithUserNotFound() {
        LoginRequest request = new LoginRequest(
                "victor@email.com",
                "12345678"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest(
                "victor@email.com",
                "senha-errada"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(jwtService, never()).generateToken(any(User.class));
    }
}