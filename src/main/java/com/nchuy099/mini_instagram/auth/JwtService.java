package com.nchuy099.mini_instagram.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.enums.TokenType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {

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

    public String generate(TokenType type, String subject) {

        return switch (type) {
            case ACCESS -> generateToken(subject, accessKey, accessExpireMins * 60 * 1000, type);
            case REFRESH -> generateToken(subject, refreshKey, refreshExpireDays * 24 * 60 * 60 * 1000, type);
            case RESET_PASSWORD -> generateToken(subject, resetKey, resetExpireMins * 60 * 1000, type);
        };
    }

    public boolean validate(String token, TokenType expectedType) {
        log.info("Validating {} token: {}", expectedType, token);
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            String tokenType = jwt.getJWTClaimsSet()
                    .getStringClaim("token_type");

            if (!expectedType.name().equals(tokenType)) {
                log.warn("Token type mismatch: expected {}, found {}", expectedType.name(), tokenType);
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

    private String generateToken(String sub, String key, long expireMillis, TokenType type) {
        log.info("Generating {} token for subject: {}", type, sub);
        try {
            // 1️⃣ Header
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

            // 2️⃣ Time
            Date issuedAt = new Date();
            Date expiration = new Date(
                    issuedAt.getTime() + expireMillis);

            // 3️⃣ Claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(sub)
                    .issueTime(issuedAt)
                    .expirationTime(expiration)
                    .claim("token_type", type.name())
                    .build();

            // 4️⃣ Payload
            Payload payload = new Payload(claimsSet.toJSONObject());

            // 5️⃣ JWS Object
            JWSObject jwsObject = new JWSObject(header, payload);

            // 6️⃣ Sign
            jwsObject.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));

            // 7️⃣ Serialize
            return jwsObject.serialize();

        } catch (JOSEException e) {
            log.error("Failed to generate JWT: {}", e.getMessage());
            throw new RuntimeException("Failed to generate JWT", e);
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
