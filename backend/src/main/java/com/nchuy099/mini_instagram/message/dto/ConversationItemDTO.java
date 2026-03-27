package com.nchuy099.mini_instagram.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationItemDTO {
    private UUID id;
    private List<ConversationParticipantDTO> participants;
    private String lastMessagePreview;
    private ZonedDateTime lastMessageAt;
    private Long unreadCount;
}
