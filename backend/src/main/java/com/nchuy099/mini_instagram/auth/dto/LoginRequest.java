package com.nchuy099.mini_instagram.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Phone Number, Email or username is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
