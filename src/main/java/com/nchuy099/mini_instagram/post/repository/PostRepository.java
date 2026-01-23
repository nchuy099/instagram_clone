package com.nchuy099.mini_instagram.post.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nchuy099.mini_instagram.post.entity.PostEntity;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    @Query("""
            Select p from PostEntity p
            Where p.owner.id = :ownerId
            AND (
                p.createdAt < :createdAt
                OR (p.createdAt = :createdAt AND p.id > :id)
            )
            ORDER BY p.createdAt DESC, p.id ASC
            """)
    List<PostEntity> findFeedPosts(@Param("id") UUID id, @Param("ownerId") UUID ownerId,
            @Param("createdAt") Instant createdAt, Pageable pageable);
}
