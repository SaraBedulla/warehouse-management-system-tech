package com.wms.controller;

import com.wms.dto.request.LoginRequest;
import com.wms.dto.request.RegisterRequest;
import com.wms.dto.response.AuthResponse;
import com.wms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public endpoints — register and login, both return a ready-to-use JWT token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticate with username and password. Returns a Bearer JWT token."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register",
        description = "Create a new CLIENT account. Role is always CLIENT — managers and admins are created by SYSTEM_ADMIN. Returns a JWT token so the user is logged in immediately after registering."
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}
