package com.nchuy099.mini_instagram.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.post.dto.CreatePostRequest;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.dto.UpdatePostRequest;
import com.nchuy099.mini_instagram.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldCreatePost() throws Exception {
        CreatePostRequest.PostMediaRequest mediaRequest = new CreatePostRequest.PostMediaRequest();
        mediaRequest.setUrl("http://example.com/image.jpg");
        mediaRequest.setType("IMAGE");

        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("Test caption");
        request.setMedia(java.util.List.of(mediaRequest));

        PostDTO mockPost = PostDTO.builder()
                .id(UUID.randomUUID())
                .caption("Test caption")
                .build();

        when(postService.createPost(any(CreatePostRequest.class))).thenReturn(mockPost);

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caption").value("Test caption"));
    }

    @Test
    void shouldGetPost() throws Exception {
        UUID postId = UUID.randomUUID();
        PostDTO mockPost = PostDTO.builder()
                .id(postId)
                .caption("Test")
                .build();

        when(postService.getPostById(postId)).thenReturn(mockPost);

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(postId.toString()));
    }

    @Test
    void shouldUpdatePost() throws Exception {
        UUID postId = UUID.randomUUID();
        UpdatePostRequest request = new UpdatePostRequest();
        request.setCaption("Updated");

        PostDTO mockPost = PostDTO.builder()
                .id(postId)
                .caption("Updated")
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class))).thenReturn(mockPost);

        mockMvc.perform(patch("/api/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caption").value("Updated"));
    }

    @Test
    void shouldDeletePost() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(delete("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).deletePost(postId);
    }

    @Test
    void shouldLikePost() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(post("/api/posts/" + postId + "/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).likePost(postId);
    }

    @Test
    void shouldSavePost() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(post("/api/posts/" + postId + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).savePost(postId);
    }
}
