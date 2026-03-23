package com.nchuy099.mini_instagram.post.repository;

import com.nchuy099.mini_instagram.post.entity.PostLike;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    boolean existsByPostAndUser(Post post, User user);
    void deleteByPostAndUser(Post post, User user);
}
