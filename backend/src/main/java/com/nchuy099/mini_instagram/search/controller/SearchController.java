package com.nchuy099.mini_instagram.search.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.search.dto.RecentSearchDTO;
import com.nchuy099.mini_instagram.search.dto.SearchHashtagDTO;
import com.nchuy099.mini_instagram.search.dto.SearchPostDTO;
import com.nchuy099.mini_instagram.search.dto.SearchResultDTO;
import com.nchuy099.mini_instagram.search.dto.SearchUserDTO;
import com.nchuy099.mini_instagram.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultDTO>> searchAll(@RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchAll(query)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<SearchUserDTO>>> searchUsers(@RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchUsers(query)));
    }

    @GetMapping("/hashtags")
    public ResponseEntity<ApiResponse<List<SearchHashtagDTO>>> searchHashtags(@RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchHashtags(query)));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<SearchPostDTO>>> searchPosts(@RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchPosts(query)));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RecentSearchDTO>>> getRecentSearches() {
        return ResponseEntity.ok(ApiResponse.success(searchService.getRecentSearches()));
    }

    @PostMapping("/recent/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> trackUserResultClick(
            @PathVariable UUID userId,
            @RequestParam(value = "q", required = false) String queryText
    ) {
        searchService.trackUserResultClick(userId, queryText);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).build());
    }

    @DeleteMapping("/recent/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(@PathVariable UUID id) {
        searchService.deleteRecentSearch(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Recent search deleted")
                .build());
    }
}
