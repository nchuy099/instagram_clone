package com.nchuy099.mini_instagram.post.service;

import com.nchuy099.mini_instagram.post.dto.CreatePostRequest;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.dto.UpdatePostRequest;
import com.nchuy099.mini_instagram.common.response.PagedResponse;

import java.util.UUID;

public interface PostService {
    PostDTO createPost(CreatePostRequest request);
    PostDTO getPostById(UUID postId);
    PostDTO updatePost(UUID postId, UpdatePostRequest request);
    void deletePost(UUID postId);
    
    PagedResponse<PostDTO> getFeed(int page, int size);
    PagedResponse<PostDTO> getUserPosts(String username, int page, int size);
    PagedResponse<PostDTO> getSavedPosts(int page, int size);
    
    // Social actions
    void likePost(UUID postId);
    void unlikePost(UUID postId);
    void savePost(UUID postId);
    void unsavePost(UUID postId);
}
