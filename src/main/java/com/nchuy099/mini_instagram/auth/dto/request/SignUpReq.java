package com.nchuy099.mini_instagram.auth.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpReq {
    @NotBlank
    @Email
    String email;

    @NotBlank
    String username;

    @NotBlank
    String fullName;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password;
}
