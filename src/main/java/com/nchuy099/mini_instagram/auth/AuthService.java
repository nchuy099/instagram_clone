package com.nchuy099.mini_instagram.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.auth.dto.request.LoginReq;
import com.nchuy099.mini_instagram.auth.dto.response.LoginResp;
import com.nchuy099.mini_instagram.common.enums.TokenType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.token.JwtTokenService;
import com.nchuy099.mini_instagram.token.dto.TokenResult;
import com.nchuy099.mini_instagram.token.entity.RefreshToken;
import com.nchuy099.mini_instagram.token.repository.RefreshTokenRepository;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;

    public LoginResp login(LoginReq req) {
        log.info("Login request received for identifier: {}", req.getIdentifier());
        // Implement login logic here
        Optional<UserEntity> existingUser = userRepository.findByUsernameOrEmail(req.getIdentifier(),
                req.getIdentifier());
        if (existingUser.isEmpty()) {
            log.warn("No user found with identifier: {}", req.getIdentifier());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Email/Username or password is incorrect");
        }

        UserEntity user = existingUser.get();
        String userId = user.getId().toString();
        Instant issuedAt = Instant.now();
        // create tokens
        TokenResult accessToken = jwtTokenService.generate(TokenType.ACCESS, issuedAt, userId);
        TokenResult refreshToken = jwtTokenService.generate(TokenType.REFRESH, issuedAt, userId);

        RefreshToken rftEntity = RefreshToken.builder()
                .token(refreshToken.getToken())
                .user(user)
                .expiresAt(refreshToken.getExpiresAt())
                .build();

        // save tokens to db
        refreshTokenRepository.save(rftEntity);

        return LoginResp.builder()
                .userId(userId)
                .username(user.getUsername())
                .accessToken(accessToken.getToken())
                .refreshToken(refreshToken.getToken())
                .build();

    }
}
