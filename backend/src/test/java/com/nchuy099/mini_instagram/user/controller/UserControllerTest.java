package com.nchuy099.mini_instagram.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.post.service.PostService;
import com.nchuy099.mini_instagram.user.dto.HomeSuggestionDTO;
import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.nchuy099.mini_instagram.user.service.FollowService;
import com.nchuy099.mini_instagram.user.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private FollowService followService;

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
    void shouldGetUserProfile() throws Exception {
        ProfileHeaderDTO mockProfile = ProfileHeaderDTO.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .followerCount(100)
                .build();

        when(userService.getUserProfile("testuser")).thenReturn(mockProfile);

        mockMvc.perform(get("/api/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.followerCount").value(100));
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("New bio");

        UserDTO updatedUser = UserDTO.builder()
                .id(UUID.randomUUID())
                .username("currentuser")
                .bio("New bio")
                .build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/api/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bio").value("New bio"));
    }

    @Test
    void shouldGetFollowers() throws Exception {
        UserDTO follower = UserDTO.builder().username("follower").build();
        PagedResponse<UserDTO> pagedResponse = PagedResponse.<UserDTO>builder()
                .content(List.of(follower))
                .build();

        when(followService.getFollowers(eq("testuser"), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/users/testuser/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("follower"));
    }

    @Test
    void shouldGetFollowing() throws Exception {
        UserDTO following = UserDTO.builder().username("following").build();
        PagedResponse<UserDTO> pagedResponse = PagedResponse.<UserDTO>builder()
                .content(List.of(following))
                .build();

        when(followService.getFollowing(eq("testuser"), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/users/testuser/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("following"));
    }

    @Test
    void shouldFollowUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/" + userId + "/follow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Followed user successfully"));
        
        verify(followService).followUser(userId);
    }

    @Test
    void shouldUnfollowUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/" + userId + "/follow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Unfollowed user successfully"));
        
        verify(followService).unfollowUser(userId);
    }

    @Test
    void shouldGetHomeSuggestions() throws Exception {
        HomeSuggestionDTO suggestion = HomeSuggestionDTO.builder()
                .id(UUID.randomUUID())
                .username("leomessi")
                .fullName("Leo Messi")
                .subtitle("Suggested for you")
                .build();

        when(userService.getHomeSuggestions()).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/users/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("leomessi"));
    }
}
