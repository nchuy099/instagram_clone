package com.nchuy099.mini_instagram.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String bio;
    private String avatarUrl;
    private String websiteUrl;
    private Boolean isPrivate;
}
