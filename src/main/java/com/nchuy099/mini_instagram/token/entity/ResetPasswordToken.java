package com.nchuy099.mini_instagram.token.entity;

import java.time.Instant;
import java.util.UUID;

import com.nchuy099.mini_instagram.common.AbstractEntity;
import com.nchuy099.mini_instagram.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Table(name = "password_resets")
@Entity
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordToken extends AbstractEntity {

    @Column(nullable = false, unique = true)
    UUID jti;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    @Builder.Default
    boolean used = false;

    @Column(nullable = false)
    Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

}
