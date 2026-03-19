package com.nchuy099.mini_instagram.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileHeaderDTO {
    private Long id;
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
