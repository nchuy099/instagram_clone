package com.nchuy099.mini_instagram.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEventDTO {
    private String type;
    private NotificationDTO notification;
}
