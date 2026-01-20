package com.nchuy099.mini_instagram.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class LoginResp {
    String userId;
    String username;
    String avatarUrl;
    String accessToken;
    String refreshToken;
}
