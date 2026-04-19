package com.nchuy099.mini_instagram.message.service;

import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageServiceImpl messageService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User authenticateCurrentUser() {
        String principal = "me@example.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
        );

        User currentUser = User.builder()
                .id(UUID.randomUUID())
                .username("me")
                .email(principal)
                .build();

        when(userService.getByUsername(principal)).thenReturn(currentUser);
        return currentUser;
    }

    @Test
    void createOrGetConversation_WhenNoExisting_ShouldCreateConversation() {
        User currentUser = authenticateCurrentUser();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").email("other@example.com").build();

        Conversation createdConversation = Conversation.builder().id(UUID.randomUUID()).build();

        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(conversationRepository.findDirectConversationId(currentUser.getId(), otherUser.getId())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(createdConversation);

        ConversationParticipant currentMembership = ConversationParticipant.builder()
                .conversation(createdConversation)
                .user(currentUser)
                .lastReadAt(LocalDateTime.now())
                .build();
        ConversationParticipant otherMembership = ConversationParticipant.builder()
                .conversation(createdConversation)
                .user(otherUser)
                .lastReadAt(LocalDateTime.now())
                .build();

        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(currentMembership);
        when(conversationParticipantRepository.findByConversation(createdConversation)).thenReturn(List.of(currentMembership, otherMembership));
        when(messageRepository.findTopByConversationOrderByCreatedAtDesc(createdConversation)).thenReturn(Optional.empty());
        when(messageRepository.countUnreadMessages(eq(createdConversation), eq(currentUser.getId()), any())).thenReturn(0L);

        ConversationItemDTO result = messageService.createOrGetConversation(otherUser.getId());

        assertThat(result.getId()).isEqualTo(createdConversation.getId());
        assertThat(result.getParticipants()).hasSize(1);
        assertThat(result.getParticipants().get(0).getUsername()).isEqualTo("other");
    }

    @Test
    void sendMessage_WhenParticipant_ShouldSaveAndPublishRealtimeEvent() {
        User currentUser = authenticateCurrentUser();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").email("other@example.com").build();

        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();
        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(currentUser)
                .content("hello")
                .build();
        savedMessage.setCreatedAt(LocalDateTime.now());

        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
        ConversationParticipant otherMembership = ConversationParticipant.builder().conversation(conversation).user(otherUser).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)).thenReturn(Optional.of(currentMembership));
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership, otherMembership));

        MessageDTO result = messageService.sendMessage(conversation.getId(), "hello", null);

        assertThat(result.getContent()).isEqualTo("hello");
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversation.getId()), any(Object.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(any(), eq("/queue/messages"), any());
    }

    @Test
    void sendMessage_WhenSharedPostOnly_ShouldSucceed() {
        User currentUser = authenticateCurrentUser();
        User postOwner = User.builder().id(UUID.randomUUID()).username("owner").avatarUrl("owner-avatar").build();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").email("other@example.com").build();
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();

        Post sharedPost = Post.builder()
                .id(UUID.randomUUID())
                .user(postOwner)
                .caption("caption")
                .media(List.of(PostMedia.builder().url("media-url").type(PostMedia.MediaType.IMAGE).orderIndex(0).build()))
                .build();

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(currentUser)
                .content("")
                .sharedPost(sharedPost)
                .build();
        savedMessage.setCreatedAt(LocalDateTime.now());

        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
        ConversationParticipant otherMembership = ConversationParticipant.builder().conversation(conversation).user(otherUser).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser)).thenReturn(true);
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership, otherMembership));
        when(postRepository.findById(sharedPost.getId())).thenReturn(Optional.of(sharedPost));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)).thenReturn(Optional.of(currentMembership));

        MessageDTO result = messageService.sendMessage(conversation.getId(), "   ", sharedPost.getId());

        assertThat(result.getSharedPost()).isNotNull();
        assertThat(result.getSharedPost().getPostId()).isEqualTo(sharedPost.getId());
        assertThat(result.getSharedPost().getMediaUrl()).isEqualTo("media-url");
    }

    @Test
    void sendMessage_WhenContentEmptyAndNoSharedPost_ShouldFail() {
        User currentUser = authenticateCurrentUser();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").email("other@example.com").build();
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();

        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
        ConversationParticipant otherMembership = ConversationParticipant.builder().conversation(conversation).user(otherUser).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser)).thenReturn(true);
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership, otherMembership));

        assertThatThrownBy(() -> messageService.sendMessage(conversation.getId(), "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content or shared post is required");
    }

    @Test
    void sendStoryReplyMessage_WhenValid_ShouldCreateConversationIfMissingAndPublishRealtimeEvent() {
        User currentUser = authenticateCurrentUser();
        User recipient = User.builder().id(UUID.randomUUID()).username("owner").email("owner@example.com").build();
        Story story = Story.builder()
                .id(UUID.randomUUID())
                .user(recipient)
                .mediaUrl("story-media")
                .mediaType("IMAGE")
                .build();
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .user(currentUser)
                .content("Great story")
                .sharedStory(story)
                .build();
        savedMessage.setCreatedAt(LocalDateTime.now());

        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
        ConversationParticipant otherMembership = ConversationParticipant.builder().conversation(conversation).user(recipient).build();

        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(storyRepository.findById(story.getId())).thenReturn(Optional.of(story));
        when(conversationRepository.findDirectConversationId(currentUser.getId(), recipient.getId())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(currentMembership);
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership, otherMembership));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)).thenReturn(Optional.of(currentMembership));

        MessageDTO result = messageService.sendStoryReplyMessage(recipient.getId(), story.getId(), "Great story");

        assertThat(result.getSharedStory()).isNotNull();
        assertThat(result.getSharedStory().getStoryId()).isEqualTo(story.getId());
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversation.getId()), any(Object.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(any(), eq("/queue/messages"), any());
    }

    @Test
    void sendMessage_WhenConversationHasOnlyCurrentUser_ShouldThrowException() {
        User currentUser = authenticateCurrentUser();
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();

        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser)).thenReturn(true);
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership));

        assertThatThrownBy(() -> messageService.sendMessage(conversation.getId(), "hello", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot send message to yourself");

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void markConversationRead_WhenParticipant_ShouldPublishReadEvent() {
        User currentUser = authenticateCurrentUser();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").email("other@example.com").build();

        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();
        ConversationParticipant currentMembership = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
        ConversationParticipant otherMembership = ConversationParticipant.builder().conversation(conversation).user(otherUser).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversationAndUser(conversation, currentUser)).thenReturn(true);
        when(conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)).thenReturn(Optional.of(currentMembership));
        when(conversationParticipantRepository.findByConversation(conversation)).thenReturn(List.of(currentMembership, otherMembership));

        messageService.markConversationRead(conversation.getId());

        verify(conversationParticipantRepository).save(currentMembership);
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversation.getId()), any(Object.class));

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(any(), destinationCaptor.capture(), any());
        assertThat(destinationCaptor.getAllValues()).allMatch(value -> value.equals("/queue/messages"));
    }
}
