package com.nchuy099.mini_instagram.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProfileHeaderDTO {
    private UUID id;
    private String username;
    private String fullName;
    private String bio;
    private String avatarUrl;
    
    // Stats
    private Integer postCount;
    private Integer followerCount;
    private Integer followingCount;
    
    // Viewer relation state
    private Boolean isFollowing;
    private Boolean isOwnProfile;
}
