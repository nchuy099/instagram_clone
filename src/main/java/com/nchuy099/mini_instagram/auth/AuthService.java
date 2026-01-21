package com.nchuy099.mini_instagram.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nchuy099.mini_instagram.auth.dto.request.LoginReq;
import com.nchuy099.mini_instagram.auth.dto.response.LoginResp;
import com.nchuy099.mini_instagram.auth.dto.response.RefreshTokenResp;
import com.nchuy099.mini_instagram.common.enums.TokenType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.token.JwtTokenService;
import com.nchuy099.mini_instagram.token.dto.TokenResult;
import com.nchuy099.mini_instagram.token.entity.BlackListToken;
import com.nchuy099.mini_instagram.token.entity.RefreshToken;
import com.nchuy099.mini_instagram.token.repository.BlackListTokenRepository;
import com.nchuy099.mini_instagram.token.repository.RefreshTokenRepository;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    @Value("${send-mail-service.url}")
    private String SEND_RESET_PASSWORD_EMAIL_URL;

    @Value("${frontend.url}")
    private String FRONTEND_URL;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlackListTokenRepository blackListTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public LoginResp login(LoginReq req) {
        log.info("Login request received for identifier: {}", req.getIdentifier());
        // Implement login logic here
        Optional<UserEntity> existingUser = userRepository.findByUsernameOrEmail(req.getIdentifier(),
                req.getIdentifier());
        if (existingUser.isEmpty()) {
            log.warn("No user found with identifier: {}", req.getIdentifier());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Email/Username or password is incorrect");
        }

        if (!passwordEncoder.matches(req.getPassword(), existingUser.get().getPassword())) {
            log.warn("Invalid password for user: {}", req.getIdentifier());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Email/Username or password is incorrect");
        }

        UserEntity user = existingUser.get();
        String userId = user.getId().toString();
        Instant issuedAt = Instant.now();
        // create tokens
        TokenResult accessToken = jwtTokenService.generate(TokenType.ACCESS, issuedAt, userId, UUID.randomUUID());
        UUID refreshJti = UUID.randomUUID();
        TokenResult refreshToken = jwtTokenService.generate(TokenType.REFRESH, issuedAt, userId, refreshJti);

        RefreshToken rftEntity = RefreshToken.builder()
                .jti(refreshJti)
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

    public void forgotPassword(String identifier) {
        log.info("Forgot password request received for identifier: {}", identifier);

        Optional<UserEntity> existingUser = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (existingUser.isEmpty()) {
            log.warn("No user found with identifier: {}", identifier);
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        TokenResult resetToken = jwtTokenService.generate(TokenType.RESET_PASSWORD, Instant.now(),
                existingUser.get().getId().toString(), UUID.randomUUID());

        sendResetPasswordEmail(existingUser.get().getEmail(), existingUser.get().getUsername(), resetToken.getToken());
        // create token
        // send email

    }

    public RefreshTokenResp refreshToken(String refreshToken) {
        log.info("Refresh token request received");

        // revoke old refresh token
        Jwt refreshJwt = jwtTokenService.decodeToken(TokenType.REFRESH, refreshToken);
        UUID refreshJti = UUID.fromString(refreshJwt.getClaimAsString("jti"));

        Optional<RefreshToken> existingRft = refreshTokenRepository.findByJti(refreshJti);
        if (existingRft.isEmpty()) {
            log.info("Refresh token not found in database");
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }

        existingRft.get().setRevoked(true);
        refreshTokenRepository.save(existingRft.get());

        // blacklist old access token
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Jwt accessJwt = ((JwtAuthenticationToken) auth).getToken();

        // UUID accessJti = UUID.fromString(accessJwt.getClaimAsString("jti"));

        // BlackListToken blackListToken = BlackListToken.builder()
        // .jti(accessJti)
        // .expiresAt(accessJwt.getExpiresAt())
        // .build();

        // blackListTokenRepository.save(blackListToken);

        // generate new tokens
        var newAccessToken = jwtTokenService.generate(TokenType.ACCESS, Instant.now(),
                refreshJwt.getSubject(), UUID.randomUUID()).getToken();

        UUID newRefreshJti = UUID.randomUUID();
        TokenResult newRefreshToken = jwtTokenService.generate(TokenType.REFRESH, Instant.now(),
                refreshJwt.getSubject(), newRefreshJti);

        RefreshToken newRftEntity = RefreshToken.builder()
                .jti(newRefreshJti)
                .user(existingRft.get().getUser())
                .expiresAt(newRefreshToken.getExpiresAt())
                .build();
        refreshTokenRepository.save(newRftEntity);

        return RefreshTokenResp.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    public void sendResetPasswordEmail(String email, String username, String token) {
        log.info("Sending reset password email to: {}", email);
        try {
            WebClient webClient = WebClient.create();

            ResetPasswordMailResponse response = webClient.post()
                    .uri(SEND_RESET_PASSWORD_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ResetPasswordMailReq(
                            email,
                            username,
                            token,
                            FRONTEND_URL))
                    .retrieve()

                    // ❗ HTTP status != 2xx
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("Reset password email API ERROR body: {}", body);
                                return Mono.error(
                                        new RuntimeException("Reset password email API failed: " + body));
                            }))

                    .bodyToMono(ResetPasswordMailResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Reset password email API response: {}", response.getMessage());

            // ❗ Business error (success = false)
            if (response == null || !response.isSuccess()) {
                log.error("Failed to send reset password email. Response: {}", response.getError());
                throw new RuntimeException("Failed to send reset password email: " + response.getError());
            }

        } catch (Exception ex) {
            log.error("Error calling reset password email API", ex);
            throw new RuntimeException("Internal server error while sending reset password email", ex);
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor
    class ResetPasswordMailReq {
        String email;
        String username;
        String token;
        String frontendUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ResetPasswordMailResponse {
        boolean success;
        int code;
        String message;

        ResetPasswordMailResponse(boolean success, int code, String message) {
            this.success = success;
            this.code = code;
            this.message = message;
        }

        String error;
    }
}
