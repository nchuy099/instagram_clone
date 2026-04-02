package com.nchuy099.mini_instagram.user.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.service.PostService;
import com.nchuy099.mini_instagram.user.dto.HomeSuggestionDTO;
import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UpdateUsernameRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import jakarta.validation.Valid;
import com.nchuy099.mini_instagram.user.service.FollowService;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final PostService postService;

    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<ProfileHeaderDTO>> getUserProfile(@PathVariable String username) {
        ProfileHeaderDTO profile = userService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.<ProfileHeaderDTO>builder()
                .success(true)
                .data(profile)
                .build());
    }

    @GetMapping("/users/suggestions")
    public ResponseEntity<ApiResponse<List<HomeSuggestionDTO>>> getHomeSuggestions() {
        return ResponseEntity.ok(ApiResponse.success(userService.getHomeSuggestions()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(@RequestBody UpdateProfileRequest request) {
        UserDTO updatedUser = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.<UserDTO>builder()
                .success(true)
                .data(updatedUser)
                .message("Profile updated successfully")
                .build());
    }

    @PatchMapping("/users/me/username")
    public ResponseEntity<ApiResponse<UserDTO>> updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        UserDTO updatedUser = userService.updateUsername(request.getUsername());
        return ResponseEntity.ok(ApiResponse.<UserDTO>builder()
                .success(true)
                .data(updatedUser)
                .message("Username updated successfully")
                .build());
    }

    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable UUID userId) {
        followService.followUser(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Followed user successfully")
                .build());
    }

    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable UUID userId) {
        followService.unfollowUser(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Unfollowed user successfully")
                .build());
    }

    @GetMapping("/users/{username}/followers")
    public ApiResponse<PagedResponse<UserDTO>> getFollowers(
            @PathVariable String username,
            Pageable pageable) {
        return ApiResponse.success(followService.getFollowers(username, pageable));
    }

    @GetMapping("/users/{username}/following")
    public ApiResponse<PagedResponse<UserDTO>> getFollowing(
            @PathVariable String username,
            Pageable pageable) {
        return ApiResponse.success(followService.getFollowing(username, pageable));
    }

    @GetMapping("/users/{username}/posts")
    public ApiResponse<PagedResponse<PostDTO>> getUserPosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.getUserPosts(username, page, size));
    }
}
