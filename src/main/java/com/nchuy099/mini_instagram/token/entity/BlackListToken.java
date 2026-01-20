package com.nchuy099.mini_instagram.token.entity;

import java.time.Instant;

import com.nchuy099.mini_instagram.common.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "token_blacklists")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BlackListToken extends AbstractEntity {

    @Column(nullable = false)
    String token;

    @Column(nullable = false)
    Instant expiresAt;

}
