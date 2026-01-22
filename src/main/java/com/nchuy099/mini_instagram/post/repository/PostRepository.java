package com.nchuy099.mini_instagram.post.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nchuy099.mini_instagram.post.entity.PostEntity;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {

}
