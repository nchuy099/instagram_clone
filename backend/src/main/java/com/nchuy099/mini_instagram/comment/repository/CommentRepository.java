package com.nchuy099.mini_instagram.comment.repository;

import com.nchuy099.mini_instagram.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByPostIdAndParentCommentIsNull(UUID postId, Pageable pageable);
    Page<Comment> findByParentCommentId(UUID parentCommentId, Pageable pageable);
}
