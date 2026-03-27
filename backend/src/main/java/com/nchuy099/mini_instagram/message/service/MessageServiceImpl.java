package com.nchuy099.mini_instagram.message.service;

import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.ConversationParticipantDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;
import com.nchuy099.mini_instagram.message.dto.MessageEventDTO;
import com.nchuy099.mini_instagram.message.entity.Conversation;
import com.nchuy099.mini_instagram.message.entity.ConversationParticipant;
import com.nchuy099.mini_instagram.message.entity.Message;
import com.nchuy099.mini_instagram.message.repository.ConversationParticipantRepository;
import com.nchuy099.mini_instagram.message.repository.ConversationRepository;
import com.nchuy099.mini_instagram.message.repository.MessageRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationItemDTO> getConversations() {
        User currentUser = getCurrentUser();
        List<ConversationParticipant> memberships = conversationParticipantRepository.findByUserOrderByConversationUpdatedAtDesc(currentUser);

        return memberships.stream()
                .map(ConversationParticipant::getConversation)
                .map(conversation -> mapConversationItem(conversation, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationItemDTO createOrGetConversation(UUID participantId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (otherUser.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot create conversation with yourself");
        }

        Conversation conversation = conversationRepository.findDirectConversationId(currentUser.getId(), otherUser.getId())
                .flatMap(conversationRepository::findById)
                .orElseGet(() -> createDirectConversation(currentUser, otherUser));

        return mapConversationItem(conversation, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(UUID conversationId) {
        User currentUser = getCurrentUser();
        Conversation conversation = getAccessibleConversation(conversationId, currentUser);

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(this::mapMessage)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(UUID conversationId, String content) {
        User currentUser = getCurrentUser();
        Conversation conversation = getAccessibleConversation(conversationId, currentUser);

        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedContent.isEmpty()) {
            throw new IllegalArgumentException("Message content is required");
        }

        Message savedMessage = messageRepository.save(Message.builder()
                .conversation(conversation)
                .user(currentUser)
                .content(trimmedContent)
                .build());

        conversationRepository.save(conversation);

        conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)
                .ifPresent(participant -> {
                    participant.setLastReadAt(LocalDateTime.now());
                    conversationParticipantRepository.save(participant);
                });

        MessageDTO messageDTO = mapMessage(savedMessage);
        MessageEventDTO event = MessageEventDTO.builder()
                .type("MESSAGE_CREATED")
                .conversationId(conversation.getId())
                .message(messageDTO)
                .build();

        publishConversationEvent(conversation, event);
        return messageDTO;
    }

    @Override
    @Transactional
    public void markConversationRead(UUID conversationId) {
        User currentUser = getCurrentUser();
        Conversation conversation = getAccessibleConversation(conversationId, currentUser);

        LocalDateTime now = LocalDateTime.now();
        ConversationParticipant participant = conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new IllegalStateException("Conversation membership not found"));

        participant.setLastReadAt(now);
        conversationParticipantRepository.save(participant);

        MessageEventDTO event = MessageEventDTO.builder()
                .type("MESSAGE_READ")
                .conversationId(conversation.getId())
                .readByUserId(currentUser.getId())
                .readAt(now.atZone(ZoneId.systemDefault()))
                .build();

        publishConversationEvent(conversation, event);
    }

    private Conversation createDirectConversation(User currentUser, User otherUser) {
        Conversation conversation = conversationRepository.save(Conversation.builder().build());
        LocalDateTime now = LocalDateTime.now();

        conversationParticipantRepository.save(ConversationParticipant.builder()
                .conversation(conversation)
                .user(currentUser)
                .lastReadAt(now)
                .build());

        conversationParticipantRepository.save(ConversationParticipant.builder()
                .conversation(conversation)
                .user(otherUser)
                .lastReadAt(now)
                .build());

        return conversation;
    }

    private Conversation getAccessibleConversation(UUID conversationId, User currentUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isParticipant = conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser);
        if (!isParticipant) {
            throw new IllegalArgumentException("You are not a participant of this conversation");
        }

        return conversation;
    }

    private ConversationItemDTO mapConversationItem(Conversation conversation, User currentUser) {
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversation(conversation);
        List<ConversationParticipantDTO> participantDTOs = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> user.getId().equals(currentUser.getId()) == false)
                .map(user -> ConversationParticipantDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        Message lastMessage = messageRepository.findTopByConversationOrderByCreatedAtDesc(conversation).orElse(null);
        ConversationParticipant currentMembership = participants.stream()
                .filter(participant -> participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Conversation membership not found"));

        long unreadCount = messageRepository.countUnreadMessages(conversation, currentUser.getId(), currentMembership.getLastReadAt());

        return ConversationItemDTO.builder()
                .id(conversation.getId())
                .participants(participantDTOs)
                .lastMessagePreview(lastMessage == null ? null : lastMessage.getContent())
                .lastMessageAt(lastMessage == null ? null : toZonedDateTime(lastMessage.getCreatedAt()))
                .unreadCount(unreadCount)
                .build();
    }

    private MessageDTO mapMessage(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getUser().getId())
                .senderUsername(message.getUser().getUsername())
                .senderAvatarUrl(message.getUser().getAvatarUrl())
                .content(message.getContent())
                .createdAt(toZonedDateTime(message.getCreatedAt()))
                .build();
    }

    private ZonedDateTime toZonedDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault());
    }

    private void publishConversationEvent(Conversation conversation, MessageEventDTO event) {
        String topicDestination = "/topic/conversations/" + conversation.getId();
        messagingTemplate.convertAndSend(topicDestination, event);

        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversation(conversation);
        for (ConversationParticipant participant : participants) {
            String principal = resolvePrincipal(participant.getUser());
            messagingTemplate.convertAndSendToUser(principal, "/queue/messages", event);
        }
    }

    private String resolvePrincipal(User user) {
        return user.getEmail() == null ? user.getUsername() : user.getEmail();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("Authentication required");
        }
        return userService.getByUsername(auth.getName());
    }
}
