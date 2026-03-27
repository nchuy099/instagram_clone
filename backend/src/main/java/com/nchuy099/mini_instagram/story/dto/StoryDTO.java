package com.nchuy099.mini_instagram.story.dto;

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
public class StoryDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private String userAvatarUrl;
    private String mediaUrl;
    private String mediaType;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
    private Long likeCount;
    private Long replyCount;
    private Long shareCount;
    private Boolean likedByCurrentUser;
}
