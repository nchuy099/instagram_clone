package com.nchuy099.mini_instagram.comment.service;

import com.nchuy099.mini_instagram.comment.dto.CommentDTO;
import com.nchuy099.mini_instagram.comment.dto.CreateCommentRequest;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    CommentDTO createComment(UUID postId, CreateCommentRequest request);
    void deleteComment(UUID commentId);
    PagedResponse<CommentDTO> getCommentsByPost(UUID postId, Pageable pageable);
    PagedResponse<CommentDTO> getRepliesByComment(UUID commentId, Pageable pageable);
}
