package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.dto.AuthResponse;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.dto.MessageResponse;
import com.uitmerch.backend.auth.dto.RegisterRequest;
import com.uitmerch.backend.auth.dto.VerifyEmailRequest;

public interface AuthService {

    MessageResponse register(RegisterRequest request);

    MessageResponse registerOrganizer(RegisterRequest request);

    MessageResponse verifyEmail(VerifyEmailRequest request);

    AuthResponse login(LoginRequest request);
}
