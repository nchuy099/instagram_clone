package com.nchuy099.mini_instagram.search.controller;

import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.search.dto.RecentSearchDTO;
import com.nchuy099.mini_instagram.search.dto.SearchHashtagDTO;
import com.nchuy099.mini_instagram.search.dto.SearchPostDTO;
import com.nchuy099.mini_instagram.search.dto.SearchResultDTO;
import com.nchuy099.mini_instagram.search.dto.SearchUserDTO;
import com.nchuy099.mini_instagram.search.entity.RecentSearch;
import com.nchuy099.mini_instagram.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldSearchAll() throws Exception {
        SearchResultDTO result = SearchResultDTO.builder()
                .users(List.of(SearchUserDTO.builder().id(UUID.randomUUID()).username("alice").build()))
                .hashtags(List.of(SearchHashtagDTO.builder().name("springboot").build()))
                .posts(List.of(SearchPostDTO.builder().id(UUID.randomUUID()).caption("post").build()))
                .build();

        when(searchService.searchAll("spring")).thenReturn(result);

        mockMvc.perform(get("/api/search").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users[0].username").value("alice"));
    }

    @Test
    void shouldSearchUsers() throws Exception {
        when(searchService.searchUsers("ali"))
                .thenReturn(List.of(SearchUserDTO.builder().id(UUID.randomUUID()).username("alice").build()));

        mockMvc.perform(get("/api/search/users").param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("alice"));
    }

    @Test
    void shouldSearchHashtags() throws Exception {
        when(searchService.searchHashtags("spr"))
                .thenReturn(List.of(SearchHashtagDTO.builder().name("springboot").build()));

        mockMvc.perform(get("/api/search/hashtags").param("q", "spr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("springboot"));
    }

    @Test
    void shouldSearchPosts() throws Exception {
        when(searchService.searchPosts("spring"))
                .thenReturn(List.of(SearchPostDTO.builder().id(UUID.randomUUID()).caption("post spring").build()));

        mockMvc.perform(get("/api/search/posts").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].caption").value("post spring"));
    }

    @Test
    void shouldGetRecentSearches() throws Exception {
        when(searchService.getRecentSearches())
                .thenReturn(List.of(RecentSearchDTO.builder()
                        .id(UUID.randomUUID())
                        .queryText("alice")
                        .searchType(RecentSearch.SearchType.USER)
                        .createdAt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/api/search/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].queryText").value("alice"));
    }

    @Test
    void shouldDeleteRecentSearch() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/search/recent/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchService).deleteRecentSearch(id);
    }

    @Test
    void shouldTrackUserRecentOnClick() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/search/recent/users/" + userId)
                        .param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchService).trackUserResultClick(userId, "alice");
    }
}
