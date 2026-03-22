package com.wms.service;

import com.wms.dto.request.LoginRequest;
import com.wms.dto.request.RegisterRequest;
import com.wms.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
}
