package com.nchuy099.mini_instagram.post.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.post.dto.CreatePostRequest;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.dto.UpdatePostRequest;
import com.nchuy099.mini_instagram.post.service.PostService;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostDTO>> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostDTO postDTO = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postDTO, "Post created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PostDTO>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getFeed(page, size)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable UUID postId) {
        PostDTO postDTO = postService.getPostById(postId);
        return ResponseEntity.ok(ApiResponse.success(postDTO));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        PostDTO postDTO = postService.updatePost(postId, request);
        return ResponseEntity.ok(ApiResponse.success(postDTO, "Post updated successfully"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> likePost(@PathVariable UUID postId) {
        postService.likePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Post liked successfully"));
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikePost(@PathVariable UUID postId) {
        postService.unlikePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Post unliked successfully"));
    }

    @PostMapping("/{postId}/save")
    public ResponseEntity<ApiResponse<Void>> savePost(@PathVariable UUID postId) {
        postService.savePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Post saved successfully"));
    }

    @DeleteMapping("/{postId}/save")
    public ResponseEntity<ApiResponse<Void>> unsavePost(@PathVariable UUID postId) {
        postService.unsavePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Post unsaved successfully"));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<PagedResponse<PostDTO>>> getSavedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(postService.getSavedPosts(page, size)));
    }
}
