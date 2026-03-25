package com.nchuy099.mini_instagram.feed.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.feed.service.FeedService;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PagedResponse<PostDTO>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(feedService.getFollowingFeed(page, size)));
    }

    @GetMapping("/explore")
    public ResponseEntity<ApiResponse<PagedResponse<PostDTO>>> getExplore(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(feedService.getExploreFeed(page, size)));
    }
}
