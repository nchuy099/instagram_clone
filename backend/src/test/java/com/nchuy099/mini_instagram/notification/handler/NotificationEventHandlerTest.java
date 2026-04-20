package com.nchuy099.mini_instagram.notification.handler;

import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.dto.NotificationEventDTO;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import com.nchuy099.mini_instagram.notification.event.CommentCreatedEvent;
import com.nchuy099.mini_instagram.notification.event.PostLikedEvent;
import com.nchuy099.mini_instagram.notification.event.UserFollowedEvent;
import com.nchuy099.mini_instagram.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationEventHandler notificationEventHandler;

    @Test
    void handlePostLikedEvent_WhenValid_ShouldCreateAndPushNotification() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        PostLikedEvent event = new PostLikedEvent(
                actorId,
                "actor",
                "https://example.com/actor.jpg",
                recipientId,
                "recipient-principal",
                postId
        );

        NotificationDTO notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.POST_LIKE)
                .actorId(actorId)
                .actorUsername("actor")
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        when(notificationService.createNotification(NotificationType.POST_LIKE, actorId, recipientId, postId, null))
                .thenReturn(notification);

        notificationEventHandler.handlePostLikedEvent(event);

        verify(notificationService).createNotification(NotificationType.POST_LIKE, actorId, recipientId, postId, null);

        ArgumentCaptor<NotificationEventDTO> payloadCaptor = ArgumentCaptor.forClass(NotificationEventDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("recipient-principal"),
                eq("/queue/notifications"),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue().getType()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(payloadCaptor.getValue().getNotification().getId()).isEqualTo(notification.getId());
    }

    @Test
    void handleCommentCreatedEvent_WhenValid_ShouldCreateAndPushNotification() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        CommentCreatedEvent event = new CommentCreatedEvent(
                actorId,
                "actor",
                "https://example.com/actor.jpg",
                recipientId,
                "recipient-principal",
                postId,
                commentId
        );

        NotificationDTO notification = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.POST_COMMENT)
                .actorId(actorId)
                .actorUsername("actor")
                .postId(postId)
                .commentId(commentId)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();

        when(notificationService.createNotification(NotificationType.POST_COMMENT, actorId, recipientId, postId, commentId))
                .thenReturn(notification);

        notificationEventHandler.handleCommentCreatedEvent(event);

        verify(notificationService).createNotification(NotificationType.POST_COMMENT, actorId, recipientId, postId, commentId);
        verify(messagingTemplate).convertAndSendToUser(
                eq("recipient-principal"),
                eq("/queue/notifications"),
                any(NotificationEventDTO.class)
        );
    }

    @Test
    void handleUserFollowedEvent_WhenSelfAction_ShouldSkip() {
        UUID actorId = UUID.randomUUID();
        UserFollowedEvent event = new UserFollowedEvent(
                actorId,
                "actor",
                "https://example.com/actor.jpg",
                actorId,
                "recipient-principal"
        );

        notificationEventHandler.handleUserFollowedEvent(event);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}
