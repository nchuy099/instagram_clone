package com.nchuy099.mini_instagram.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.service.FollowService;
import com.nchuy099.mini_instagram.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
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
                .id(1L)
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
                .id(1L)
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
}
