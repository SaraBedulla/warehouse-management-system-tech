package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.dto.request.LoginRequest;
import com.wms.dto.request.RegisterRequest;
import com.wms.dto.response.AuthResponse;
import com.wms.exception.BusinessException;
import com.wms.security.JwtAuthEntryPoint;
import com.wms.security.JwtAuthFilter;
import com.wms.security.JwtUtils;
import com.wms.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean UserDetailsService userDetailsService;

    private final AuthResponse sampleResponse = AuthResponse.builder()
            .token("jwt-token").tokenType("Bearer")
            .username("client1").fullName("Alice").role("CLIENT").build();

    @Test
    @DisplayName("POST /api/auth/login — returns 200 and token on success")
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("client1"); req.setPassword("client123");

        when(authService.login(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("client1"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    @DisplayName("POST /api/auth/login — returns 400 when fields are blank")
    void login_validationFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — returns 201 and token on success")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setFullName("New User");
        req.setEmail("new@wms.com");

        AuthResponse sampleResponse = new AuthResponse();
        sampleResponse.setToken("test-token");

        when(authService.register(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }
    @Test
    @DisplayName("POST /api/auth/register — returns 400 when username is taken")
    void register_duplicateUsername() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("client1"); req.setPassword("pass123");
        req.setFullName("Alice"); req.setEmail("alice@wms.com");

        when(authService.register(any())).thenThrow(new BusinessException("Username 'client1' is already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username 'client1' is already taken"));
    }
}
