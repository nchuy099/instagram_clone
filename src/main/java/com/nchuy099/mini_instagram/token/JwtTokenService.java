package com.nchuy099.mini_instagram.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.enums.TokenType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.token.dto.TokenResult;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    @Value("${jwt.access.key}")
    private String accessKey;

    @Value("${jwt.refresh.key}")
    private String refreshKey;

    @Value("${jwt.reset.key}")
    private String resetKey;

    @Value("${jwt.access.expireMins}")
    private long accessExpireMins;

    @Value("${jwt.refresh.expireDays}")
    private long refreshExpireDays;

    @Value("${jwt.reset.expireMins}")
    private long resetExpireMins;

    private final UserRepository userRepository;

    public TokenResult generate(TokenType type, Instant issuedAt, String subject, UUID jti) {

        Instant expiresAt = switch (type) {
            case ACCESS -> issuedAt.plus(accessExpireMins, ChronoUnit.MINUTES);
            case REFRESH -> issuedAt.plus(refreshExpireDays, ChronoUnit.DAYS);
            case RESET_PASSWORD -> issuedAt.plus(resetExpireMins, ChronoUnit.MINUTES);
        };
        String token = generateToken(subject, jti, expiresAt, type);

        return TokenResult.builder()
                .type(type)
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    public Jwt decodeToken(TokenType type, String token) {
        log.info("Decoding {} token", type);

        var res = validate(token, type);
        if (!res) {
            log.error("Invalid JWT token");
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid JWT token");
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(getKey(type).getBytes(), "HmacSHA256");
        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return nimbusJwtDecoder.decode(token);
    }

    public boolean validate(String token, TokenType expectedType) {
        log.info("Validating {} token: {}", expectedType, token);
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            UUID userId = UUID.fromString(jwt.getJWTClaimsSet()
                    .getSubject());

            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for ID: {}", userId);
                return false;
            }

            String tokenType = jwt.getJWTClaimsSet()
                    .getStringClaim("token_type");

            if (!expectedType.name().equals(tokenType)) {
                log.warn("Token type mismatch: expected {}, found {}", expectedType.name(), tokenType);
                return false;
            }

            Instant exp = jwt.getJWTClaimsSet()
                    .getExpirationTime()
                    .toInstant();

            if (Instant.now().isAfter(exp)) {
                log.warn("Token expired");
                return false;
            }

            // verify signature
            MACVerifier verifier = new MACVerifier(getKey(expectedType));
            return jwt.verify(verifier);

        } catch (Exception e) {
            log.error("Failed to validate token: {}", e.getMessage());
            return false;
        }
    }

    private String generateToken(String sub, UUID jti, Instant expiresAt, TokenType type) {
        log.info("Generating {} token for subject: {}", type, sub);
        try {
            // 1️⃣ Header
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

            // 2️⃣ Time
            Date issuedAt = new Date();
            Date expiration = Date.from(expiresAt);

            // 3️⃣ Claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(sub)
                    .issueTime(issuedAt)
                    .expirationTime(expiration)
                    .jwtID(jti.toString())
                    .claim("token_type", type.name())
                    .build();

            // 4️⃣ Payload
            Payload payload = new Payload(claimsSet.toJSONObject());

            // 5️⃣ JWS Object
            JWSObject jwsObject = new JWSObject(header, payload);

            // 6️⃣ Sign
            String key = getKey(type);
            jwsObject.sign(new MACSigner(key.getBytes()));

            // 7️⃣ Serialize
            return jwsObject.serialize();

        } catch (JOSEException e) {
            log.error("Failed to generate JWT: {}", e.getMessage());
            throw new RuntimeException("Failed to generate JWT: " + e.getMessage(), e);
        }
    }

    private String getKey(TokenType type) {
        return switch (type) {
            case ACCESS -> accessKey;
            case REFRESH -> refreshKey;
            case RESET_PASSWORD -> resetKey;
        };
    }

}
