package com.nchuy099.mini_instagram.comment.controller;

import com.nchuy099.mini_instagram.comment.dto.CommentDTO;
import com.nchuy099.mini_instagram.comment.dto.CreateCommentRequest;
import com.nchuy099.mini_instagram.comment.service.CommentService;
import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentDTO commentDTO = commentService.createComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentDTO, "Comment created successfully"));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted successfully"));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PagedResponse<CommentDTO>>> getCommentsByPost(
            @PathVariable UUID postId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CommentDTO> comments = commentService.getCommentsByPost(postId, pageable);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<PagedResponse<CommentDTO>>> getRepliesByComment(
            @PathVariable UUID commentId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CommentDTO> replies = commentService.getRepliesByComment(commentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }
}
