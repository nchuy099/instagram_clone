package com.nchuy099.mini_instagram.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutReq {
    String refreshToken;
}
