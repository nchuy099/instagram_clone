package com.nchuy099.mini_instagram.token.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nchuy099.mini_instagram.token.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

}
