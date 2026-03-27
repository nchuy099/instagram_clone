package com.nchuy099.mini_instagram.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipantDTO {
    private UUID id;
    private String username;
    private String avatarUrl;
}
