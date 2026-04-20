package com.nchuy099.mini_instagram.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDTO {
    private UUID id;
    private NotificationType type;
    private LocalDateTime createdAt;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime readAt;
    private UUID actorId;
    private String actorUsername;
    private String actorAvatarUrl;
    private UUID postId;
    private UUID commentId;
}
