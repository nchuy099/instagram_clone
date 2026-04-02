package com.nchuy099.mini_instagram.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HomeSuggestionDTO {
    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String subtitle;
}

