package com.nchuy099.mini_instagram.auth.repository;

import com.nchuy099.mini_instagram.auth.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, UUID> {
    Optional<UserRefreshToken> findByRefreshTokenHash(String refreshTokenHash);
}
