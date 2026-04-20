package com.nchuy099.mini_instagram.notification.service;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;

import java.util.UUID;

public interface NotificationService {
    PagedResponse<NotificationDTO> getNotifications(int page, int size, boolean unreadOnly);

    void markAsRead(UUID notificationId);
    int markAllAsRead();

    NotificationDTO createNotification(
            NotificationType type,
            UUID actorId,
            UUID recipientId,
            UUID postId,
            UUID commentId
    );
}
