package com.nchuy099.mini_instagram.user.repository;

import com.nchuy099.mini_instagram.user.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Page<Follow> findByFollowingUsername(String username, Pageable pageable);
    Page<Follow> findByFollowerUsername(String username, Pageable pageable);
}
