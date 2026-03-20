package com.nchuy099.mini_instagram.auth.controller;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.auth.dto.TokenRequest;
import com.nchuy099.mini_instagram.auth.service.AuthService;
import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("User registered successfully")
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse token = authService.authenticateUser(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(token)
                .message("Logged in successfully")
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody TokenRequest request) {
        AuthResponse token = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .data(token)
                .message("Token refreshed successfully")
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        UserDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.<UserDTO>builder()
                .success(true)
                .data(user)
                .build());
    }
}
