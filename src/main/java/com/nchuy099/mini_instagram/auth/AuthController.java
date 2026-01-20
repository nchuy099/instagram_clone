package com.nchuy099.mini_instagram.auth;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.auth.dto.request.LoginReq;
import com.nchuy099.mini_instagram.auth.dto.request.SignUpReq;
import com.nchuy099.mini_instagram.auth.dto.response.LoginResp;
import com.nchuy099.mini_instagram.user.UserService;
import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/sign-up")
    public String signUp(@Valid @RequestBody SignUpReq req) {
        log.info("Sign up request received: {}", req.getEmail());

        CreateUserReq createUserReq = CreateUserReq.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .fullName(req.getFullName())
                .password(req.getPassword())
                .build();

        return userService.create(createUserReq);

    }

    @PostMapping("/login")
    public LoginResp login(@RequestBody LoginReq req) {
        log.info("Login request received: {}", req.getIdentifier());
        return authService.login(req);
    }
}
