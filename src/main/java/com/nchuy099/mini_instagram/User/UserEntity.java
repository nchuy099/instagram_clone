package com.nchuy099.mini_instagram.user;

import java.time.LocalDate;
import java.util.Date;

import com.nchuy099.mini_instagram.common.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "\"User\"")
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

    @Column(name = "\"fullName\"")
    String fullName;

    String password;

    @Column(nullable = true)
    String biography;

    @Column(name = "\"dateOfBirth\"", nullable = true)
    LocalDate dateOfBirth;

    @Column(nullable = true)
    String gender;

    @Column(name = "\"phoneNumber\"", nullable = true, length = 15)
    String phoneNumber;

}
