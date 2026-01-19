package com.nchuy099.mini_instagram.common.security;

import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.nchuy099.mini_instagram.auth.JwtService;
import com.nchuy099.mini_instagram.common.enums.TokenType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.access.key}")
    private String accessKey;

    NimbusJwtDecoder nimbusJwtDecoder = null;

    private final JwtService jwtService;

    @Override
    public Jwt decode(String token) {
        log.info("Decoding token: {}", token);

        var res = jwtService.validate(token, TokenType.ACCESS);
        if (!res) {
            log.error("Invalid JWT token");
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid JWT token");
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(accessKey.getBytes(), "HmacSHA256");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
