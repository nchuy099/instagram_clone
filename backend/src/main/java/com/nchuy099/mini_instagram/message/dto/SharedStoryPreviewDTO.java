package com.nchuy099.mini_instagram.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedStoryPreviewDTO {
    private UUID storyId;
    private String ownerUsername;
    private String ownerAvatarUrl;
    private String mediaUrl;
    private String mediaType;
    private ZonedDateTime expiresAt;
    private boolean expired;
}
