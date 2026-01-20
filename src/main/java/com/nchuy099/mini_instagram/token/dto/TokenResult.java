package com.nchuy099.mini_instagram.token.dto;

import java.time.Instant;

import com.nchuy099.mini_instagram.common.enums.TokenType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResult {
    TokenType type;
    String token;
    Instant expiresAt;
}
