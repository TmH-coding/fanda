package com.fanda.service;

import com.fanda.dto.request.LoginRequest;
import com.fanda.dto.request.RegisterRequest;
import com.fanda.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}
