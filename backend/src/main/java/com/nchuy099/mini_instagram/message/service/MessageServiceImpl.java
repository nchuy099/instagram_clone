package com.nchuy099.mini_instagram.message.service;

import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.ConversationParticipantDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;
import com.nchuy099.mini_instagram.message.dto.MessageEventDTO;
import com.nchuy099.mini_instagram.message.dto.SharedPostPreviewDTO;
import com.nchuy099.mini_instagram.message.dto.SharedStoryPreviewDTO;
import com.nchuy099.mini_instagram.message.entity.Conversation;
import com.nchuy099.mini_instagram.message.entity.ConversationParticipant;
import com.nchuy099.mini_instagram.message.entity.Message;
import com.nchuy099.mini_instagram.message.repository.ConversationParticipantRepository;
import com.nchuy099.mini_instagram.message.repository.ConversationRepository;
import com.nchuy099.mini_instagram.message.repository.MessageRepository;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.entity.PostMedia;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.repository.StoryRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final PostRepository postRepository;
    private final StoryRepository storyRepository;
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
    public MessageDTO sendMessage(UUID conversationId, String content, UUID sharedPostId) {
        User currentUser = getCurrentUser();
        Conversation conversation = getAccessibleConversation(conversationId, currentUser);
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversation(conversation);

        boolean hasOtherParticipant = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(currentUser.getId()) == false);
        if (!hasOtherParticipant) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        String trimmedContent = content == null ? "" : content.trim();
        Post sharedPost = null;
        if (sharedPostId != null) {
            sharedPost = postRepository.findById(sharedPostId)
                    .orElseThrow(() -> new IllegalArgumentException("Shared post not found"));
        }

        if (trimmedContent.isEmpty() && sharedPost == null) {
            throw new IllegalArgumentException("Message content or shared post is required");
        }

        return persistAndPublishMessage(conversation, participants, currentUser, trimmedContent, sharedPost, null);
    }

    @Override
    @Transactional
    public MessageDTO sendStoryReplyMessage(UUID recipientUserId, UUID storyId, String content) {
        User currentUser = getCurrentUser();
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        if (recipient.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedContent.isEmpty()) {
            throw new IllegalArgumentException("Reply content is required");
        }

        Story sharedStory = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("Shared story not found"));

        Conversation conversation = conversationRepository.findDirectConversationId(currentUser.getId(), recipient.getId())
                .flatMap(conversationRepository::findById)
                .orElseGet(() -> createDirectConversation(currentUser, recipient));
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversation(conversation);

        boolean hasOtherParticipant = participants.stream()
                .anyMatch(participant -> participant.getUser().getId().equals(currentUser.getId()) == false);
        if (!hasOtherParticipant) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        return persistAndPublishMessage(conversation, participants, currentUser, trimmedContent, null, sharedStory);
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
                .lastMessagePreview(resolveLastMessagePreview(lastMessage))
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
                .sharedPostId(message.getSharedPost() == null ? null : message.getSharedPost().getId())
                .sharedPost(mapSharedPostPreview(message.getSharedPost()))
                .sharedStoryId(message.getSharedStory() == null ? null : message.getSharedStory().getId())
                .sharedStory(mapSharedStoryPreview(message.getSharedStory()))
                .createdAt(toZonedDateTime(message.getCreatedAt()))
                .build();
    }

    private String resolveLastMessagePreview(Message lastMessage) {
        if (lastMessage == null) {
            return null;
        }
        if (lastMessage.getContent() != null && !lastMessage.getContent().isBlank()) {
            return lastMessage.getContent();
        }
        if (lastMessage.getSharedPost() != null) {
            return "Shared a post";
        }
        if (lastMessage.getSharedStory() != null) {
            if (lastMessage.getSharedStory().getExpiresAt() != null
                    && lastMessage.getSharedStory().getExpiresAt().isBefore(ZonedDateTime.now())) {
                return "Story expired";
            }
            return "Replied to your story";
        }
        return lastMessage.getContent();
    }

    private SharedPostPreviewDTO mapSharedPostPreview(Post sharedPost) {
        if (sharedPost == null) {
            return null;
        }

        PostMedia previewMedia = sharedPost.getMedia().stream()
                .min(Comparator.comparing(PostMedia::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .orElse(null);

        return SharedPostPreviewDTO.builder()
                .postId(sharedPost.getId())
                .ownerUsername(sharedPost.getUser().getUsername())
                .ownerAvatarUrl(sharedPost.getUser().getAvatarUrl())
                .mediaUrl(resolvePreviewMediaUrl(previewMedia))
                .mediaType(previewMedia == null ? null : previewMedia.getType().name())
                .caption(sharedPost.getCaption())
                .build();
    }

    private SharedStoryPreviewDTO mapSharedStoryPreview(Story sharedStory) {
        if (sharedStory == null) {
            return null;
        }

        return SharedStoryPreviewDTO.builder()
                .storyId(sharedStory.getId())
                .ownerUsername(sharedStory.getUser().getUsername())
                .ownerAvatarUrl(sharedStory.getUser().getAvatarUrl())
                .mediaUrl(sharedStory.getMediaUrl())
                .mediaType(sharedStory.getMediaType())
                .expiresAt(sharedStory.getExpiresAt())
                .expired(sharedStory.getExpiresAt() != null && sharedStory.getExpiresAt().isBefore(ZonedDateTime.now()))
                .build();
    }

    private String resolvePreviewMediaUrl(PostMedia previewMedia) {
        if (previewMedia == null) {
            return null;
        }
        return previewMedia.getUrl();
    }

    private ZonedDateTime toZonedDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault());
    }

    private MessageDTO persistAndPublishMessage(
            Conversation conversation,
            List<ConversationParticipant> participants,
            User sender,
            String content,
            Post sharedPost,
            Story sharedStory
    ) {
        Message savedMessage = messageRepository.save(Message.builder()
                .conversation(conversation)
                .user(sender)
                .content(content)
                .sharedPost(sharedPost)
                .sharedStory(sharedStory)
                .build());

        conversationRepository.save(conversation);

        conversationParticipantRepository.findByConversationAndUser(conversation, sender)
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

        publishConversationEvent(participants, conversation, event);
        return messageDTO;
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

    private void publishConversationEvent(List<ConversationParticipant> participants, Conversation conversation, MessageEventDTO event) {
        String topicDestination = "/topic/conversations/" + conversation.getId();
        messagingTemplate.convertAndSend(topicDestination, event);

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
