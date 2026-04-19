package com.nchuy099.mini_instagram.user.repository;

import com.nchuy099.mini_instagram.user.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Page<Follow> findByFollowingUsername(String username, Pageable pageable);
    Page<Follow> findByFollowerUsername(String username, Pageable pageable);

    @Query("""
            SELECT f
            FROM Follow f
            JOIN FETCH f.following u
            WHERE f.follower.id = :followerId
              AND (
                :query IS NULL
                OR :query = ''
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY f.createdAt DESC
            """)
    List<Follow> findFollowingForMessageSearch(@Param("followerId") UUID followerId,
                                               @Param("query") String query,
                                               Pageable pageable);

    @Query("""
            SELECT f
            FROM Follow f
            JOIN FETCH f.follower u
            WHERE f.following.id = :followingId
              AND (
                :query IS NULL
                OR :query = ''
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY f.createdAt DESC
            """)
    List<Follow> findFollowersForMessageSearch(@Param("followingId") UUID followingId,
                                               @Param("query") String query,
                                               Pageable pageable);
}
