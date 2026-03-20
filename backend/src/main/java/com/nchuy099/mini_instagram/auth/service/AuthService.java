package com.nchuy099.mini_instagram.auth.service;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.auth.entity.UserSession;
import com.nchuy099.mini_instagram.auth.repository.UserSessionRepository;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserSessionRepository userSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

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

        User user = (User) userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String refreshToken = generateSecureToken();
        String hashedToken = hashToken(refreshToken);

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(hashedToken)
                .expiresAt(ZonedDateTime.now().plusDays(7)) // 7 days
                .build();
        userSessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String hashedToken = hashToken(refreshToken);
        
        UserSession session = userSessionRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String newJwt = tokenProvider.generateToken(session.getUser().getUsername());
        String newRefreshToken = generateSecureToken();
        String newHashedToken = hashToken(newRefreshToken);
        
        session.setRefreshTokenHash(newHashedToken);
        session.setExpiresAt(ZonedDateTime.now().plusDays(7));
        userSessionRepository.save(session);
        
        return AuthResponse.builder()
                .accessToken(newJwt)
                .refreshToken(newRefreshToken)
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
