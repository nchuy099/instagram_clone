package com.nchuy099.mini_instagram.message.service;

import com.nchuy099.mini_instagram.message.dto.ConversationItemDTO;
import com.nchuy099.mini_instagram.message.dto.MessageDTO;
import com.nchuy099.mini_instagram.message.entity.Conversation;
import com.nchuy099.mini_instagram.message.entity.ConversationParticipant;
import com.nchuy099.mini_instagram.message.entity.Message;
import com.nchuy099.mini_instagram.message.repository.ConversationParticipantRepository;
import com.nchuy099.mini_instagram.message.repository.ConversationRepository;
import com.nchuy099.mini_instagram.message.repository.MessageRepository;
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

        MessageDTO result = messageService.sendMessage(conversation.getId(), "hello");

        assertThat(result.getContent()).isEqualTo("hello");
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + conversation.getId()), any(Object.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(any(), eq("/queue/messages"), any());
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
