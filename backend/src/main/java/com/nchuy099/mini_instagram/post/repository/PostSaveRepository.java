package com.nchuy099.mini_instagram.post.repository;

import com.nchuy099.mini_instagram.post.entity.PostSave;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostSaveRepository extends JpaRepository<PostSave, UUID> {
    Optional<PostSave> findByPostAndUser(Post post, User user);
    boolean existsByPostAndUser(Post post, User user);
    void deleteByPostAndUser(Post post, User user);
    Page<PostSave> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
