package com.nchuy099.mini_instagram.story.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.story.dto.StoryDTO;
import com.nchuy099.mini_instagram.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoryDTO>> createStory(@RequestBody Map<String, String> request) {
        String mediaUrl = request.get("mediaUrl");
        String mediaType = request.get("mediaType");
        return ResponseEntity.ok(ApiResponse.success(storyService.createStory(mediaUrl, mediaType)));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getStoriesFeed() {
        return ResponseEntity.ok(ApiResponse.success(storyService.getFollowingStories()));
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<Map<String, List<StoryDTO>>>> getGroupedStories() {
        return ResponseEntity.ok(ApiResponse.success(storyService.getGroupedStories()));
    }

    @PostMapping("/{storyId}/like")
    public ResponseEntity<ApiResponse<StoryDTO>> likeStory(@PathVariable UUID storyId) {
        return ResponseEntity.ok(ApiResponse.success(storyService.likeStory(storyId), "Story liked successfully"));
    }

    @DeleteMapping("/{storyId}/like")
    public ResponseEntity<ApiResponse<StoryDTO>> unlikeStory(@PathVariable UUID storyId) {
        return ResponseEntity.ok(ApiResponse.success(storyService.unlikeStory(storyId), "Story unliked successfully"));
    }

    @PostMapping("/{storyId}/replies")
    public ResponseEntity<ApiResponse<StoryDTO>> replyToStory(@PathVariable UUID storyId, @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ApiResponse.success(storyService.replyToStory(storyId, request.get("content")), "Story replied successfully"));
    }

    @PostMapping("/{storyId}/share")
    public ResponseEntity<ApiResponse<StoryDTO>> shareStory(@PathVariable UUID storyId) {
        return ResponseEntity.ok(ApiResponse.success(storyService.shareStory(storyId), "Story shared successfully"));
    }
}
