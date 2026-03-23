package com.nchuy099.mini_instagram.auth.service;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.dto.LoginRequest;
import com.nchuy099.mini_instagram.auth.dto.RegisterRequest;
import com.nchuy099.mini_instagram.auth.entity.UserRefreshToken;
import com.nchuy099.mini_instagram.auth.repository.UserRefreshTokenRepository;
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
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken!");
        }

        String mobileOrEmail = registerRequest.getMobileOrEmail();
        String email = null;
        String phoneNumber = null;

        if (isEmail(mobileOrEmail)) {
            email = mobileOrEmail;
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email is already in use!");
            }
        } else if (isPhoneNumber(mobileOrEmail)) {
            phoneNumber = mobileOrEmail;
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Phone number is already in use!");
            }
        } else {
            throw new IllegalArgumentException("Invalid mobile number or email format!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .fullName(registerRequest.getFullName())
                .email(email)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .build();

        return userRepository.save(user);
    }

    private boolean isEmail(String input) {
        return input.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isPhoneNumber(String input) {
        return input.matches("^\\+?[0-9]{10,15}$");
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getIdentifier(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return createRefreshTokenForUser(authentication.getName());
    }

    @Transactional
    public AuthResponse createRefreshTokenForUser(String credential) {
        User user = userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String principal = user.getEmail() != null ? user.getEmail() : user.getUsername() != null ? user.getUsername() : user.getPhoneNumber();
        String jwt = tokenProvider.generateToken(principal);
        String refreshToken = generateSecureToken();
        String hashedToken = hashToken(refreshToken);

        UserRefreshToken session = UserRefreshToken.builder()
                .user(user)
                .refreshTokenHash(hashedToken)
                .expiresAt(ZonedDateTime.now().plusDays(7)) // 7 days
                .build();
        userRefreshTokenRepository.save(session);

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .phoneNumber(user.getPhoneNumber())
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

        UserRefreshToken session = userRefreshTokenRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String principal = session.getUser().getEmail() != null ? session.getUser().getEmail()
                : session.getUser().getUsername() != null ? session.getUser().getUsername() : session.getUser().getPhoneNumber();
        String newJwt = tokenProvider.generateToken(principal);
        String newRefreshToken = generateSecureToken();
        String newHashedToken = hashToken(newRefreshToken);

        session.setRefreshTokenHash(newHashedToken);
        session.setExpiresAt(ZonedDateTime.now().plusDays(7));
        userRefreshTokenRepository.save(session);

        return AuthResponse.builder()
                .accessToken(newJwt)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        String hashedToken = hashToken(refreshToken);
        UserRefreshToken session = userRefreshTokenRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(ZonedDateTime.now());
            userRefreshTokenRepository.save(session);
        }
    }

    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found");
        }

        String credential = authentication.getName();
        User user = userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
