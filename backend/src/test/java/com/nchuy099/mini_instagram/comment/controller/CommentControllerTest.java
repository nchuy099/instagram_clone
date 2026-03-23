package com.nchuy099.mini_instagram.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.comment.dto.CommentDTO;
import com.nchuy099.mini_instagram.comment.dto.CreateCommentRequest;
import com.nchuy099.mini_instagram.comment.service.CommentService;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldCreateComment() throws Exception {
        UUID postId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Test comment");

        CommentDTO mockComment = CommentDTO.builder()
                .id(UUID.randomUUID())
                .content("Test comment")
                .build();

        when(commentService.createComment(eq(postId), any(CreateCommentRequest.class))).thenReturn(mockComment);

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Test comment"));
    }

    @Test
    void shouldDeleteComment() throws Exception {
        UUID commentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(commentService).deleteComment(commentId);
    }

    @Test
    void shouldGetCommentsByPost() throws Exception {
        UUID postId = UUID.randomUUID();
        PagedResponse<CommentDTO> pagedResponse = PagedResponse.<CommentDTO>builder()
                .content(Collections.emptyList())
                .build();

        when(commentService.getCommentsByPost(eq(postId), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldGetReplies() throws Exception {
        UUID commentId = UUID.randomUUID();
        PagedResponse<CommentDTO> pagedResponse = PagedResponse.<CommentDTO>builder()
                .content(Collections.emptyList())
                .build();

        when(commentService.getRepliesByComment(eq(commentId), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/comments/" + commentId + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
