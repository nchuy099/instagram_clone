package com.nchuy099.mini_instagram.user.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CreateUserReq {

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

    String biography;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;

    String gender;

    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters long")
    String phoneNumber;
}