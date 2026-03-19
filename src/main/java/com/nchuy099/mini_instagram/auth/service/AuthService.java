package com.nchuy099.mini_instagram.auth.service;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmailOrUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication.getName());

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken("mock-refresh-token") // TODO: Implement real refresh token logic
                .build();
    }

    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();
    }
}
