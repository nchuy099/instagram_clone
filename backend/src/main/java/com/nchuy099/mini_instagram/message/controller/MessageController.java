package com.nchuy099.mini_instagram.message.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;
import com.nchuy099.mini_instagram.message.dto.MessageRequests;
import com.nchuy099.mini_instagram.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationItemDTO>>> getConversations() {
        return ResponseEntity.ok(ApiResponse.success(messageService.getConversations()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationItemDTO>> createConversation(@Valid @RequestBody MessageRequests.CreateConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(messageService.createOrGetConversation(request.getParticipantId())));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessages(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessages(conversationId)));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageDTO>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageRequests.SendMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(messageService.sendMessage(conversationId, request.getContent())));
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID conversationId) {
        messageService.markConversationRead(conversationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation marked as read"));
    }
}
