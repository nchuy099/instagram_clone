package com.nchuy099.mini_instagram.post.repository;

import com.nchuy099.mini_instagram.post.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {
    Optional<Hashtag> findByNameIgnoreCase(String name);
}
