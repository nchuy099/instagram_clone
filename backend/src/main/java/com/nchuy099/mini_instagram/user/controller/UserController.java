package com.nchuy099.mini_instagram.user.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.service.FollowService;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<ProfileHeaderDTO>> getUserProfile(@PathVariable String username) {
        ProfileHeaderDTO profile = userService.getUserProfile(username);
        return ResponseEntity.ok(ApiResponse.<ProfileHeaderDTO>builder()
                .success(true)
                .data(profile)
                .build());
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

    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable Long userId) {
        followService.followUser(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Followed user successfully")
                .build());
    }

    @DeleteMapping("/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Long userId) {
        followService.unfollowUser(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Unfollowed user successfully")
                .build());
    }
}
