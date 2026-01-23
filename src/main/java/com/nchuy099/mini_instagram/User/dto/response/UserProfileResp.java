package com.nchuy099.mini_instagram.user.dto.response;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResp {
    String userId;
    String email;
    String biography;
    String username;
    String fullName;
    LocalDate dateOfBirth;
    String gender;
    String phoneNumber;
}
