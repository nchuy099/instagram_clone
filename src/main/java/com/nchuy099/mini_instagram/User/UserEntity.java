package com.nchuy099.mini_instagram.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nchuy099.mini_instagram.common.AbstractEntity;
import com.nchuy099.mini_instagram.token.entity.RefreshToken;
import com.nchuy099.mini_instagram.token.entity.ResetPasswordToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity extends AbstractEntity {

    @Email
    @Column(unique = true)
    String email;

    @NotBlank
    @Column(unique = true)
    String username;

    String fullName;

    String password;

    @Column(nullable = true)
    String biography;

    @Column(nullable = true)
    LocalDate dateOfBirth;

    @Column(nullable = true)
    String gender;

    @Column(nullable = true, length = 15)
    String phoneNumber;

    @OneToMany(mappedBy = "user")
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ResetPasswordToken> resetPasswordTokens = new ArrayList<>();
}
