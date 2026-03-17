package com.farvic.fintech.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farvic.fintech.dto.auth.AuthResponse;
import com.farvic.fintech.dto.auth.LoginRequest;
import com.farvic.fintech.dto.auth.RegisterRequest;
import com.farvic.fintech.exception.BusinessException;
import com.farvic.fintech.exception.GlobalExceptionHandler;
import com.farvic.fintech.security.JwtAuthenticationFilter;
import com.farvic.fintech.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
    private AuthService authService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /auth/register deve retornar 200 com token")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Victor Araujo",
                "victor@email.com",
                "12345678"
        );

        AuthResponse response = new AuthResponse("jwt-token", "Bearer");

        when(authService.register(request)).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register deve retornar 400 para payload inválido")
    void shouldReturnBadRequestWhenRegisterPayloadIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "email": "email-invalido",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register deve retornar 400 quando email já existe")
    void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Victor Araujo",
                "victor@email.com",
                "12345678"
        );

        when(authService.register(request))
                .thenThrow(new BusinessException("Email already registered"));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 200 com token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest(
                "victor@email.com",
                "12345678"
        );

        AuthResponse response = new AuthResponse("jwt-token", "Bearer");

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 400 para credenciais inválidas")
    void shouldReturnBadRequestWhenLoginFails() throws Exception {
        LoginRequest request = new LoginRequest(
                "victor@email.com",
                "senha-errada"
        );

        when(authService.login(request))
                .thenThrow(new BusinessException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }
}