package com.nchuy099.mini_instagram.message.service;

import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    List<ConversationItemDTO> getConversations();
    ConversationItemDTO createOrGetConversation(UUID participantId);
    List<MessageDTO> getMessages(UUID conversationId);
    MessageDTO sendMessage(UUID conversationId, String content, UUID sharedPostId);
    MessageDTO sendStoryReplyMessage(UUID recipientUserId, UUID storyId, String content);
    void markConversationRead(UUID conversationId);
}
