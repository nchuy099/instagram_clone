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
public class MessageEventDTO {
    private String type;
    private UUID conversationId;
    private MessageDTO message;
    private UUID readByUserId;
    private ZonedDateTime readAt;
}
