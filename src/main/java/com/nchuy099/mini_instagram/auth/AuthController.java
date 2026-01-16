package com.nchuy099.mini_instagram.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.auth.dto.request.SignUpReq;
import com.nchuy099.mini_instagram.user.UserService;
import com.nchuy099.mini_instagram.user.dto.request.CreateUserReq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sign-up")
    public String signUp(@RequestBody SignUpReq req) {
        log.info("Sign up request received: {}", req.getEmail());

        CreateUserReq createUserReq = CreateUserReq.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .fullName(req.getFullName())
                .password(req.getPassword())
                .build();

        return userService.create(createUserReq);

    }
}
