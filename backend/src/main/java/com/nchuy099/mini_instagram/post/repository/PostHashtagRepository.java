package com.nchuy099.mini_instagram.post.repository;

import com.nchuy099.mini_instagram.post.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, UUID> {
}
