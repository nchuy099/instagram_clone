package com.nchuy099.mini_instagram.notification.service;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.entity.Notification;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import com.nchuy099.mini_instagram.notification.repository.NotificationRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getNotifications_ShouldReturnPagedNotificationsOrderedByNewestFirst() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("recipient").build();
        authenticateUser("recipient");

        User actor = User.builder()
                .id(UUID.randomUUID())
                .username("actor")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        Notification newest = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUser(currentUser)
                .actorUser(actor)
                .type(NotificationType.POST_LIKE)
                .postId(UUID.randomUUID())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Notification older = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUser(currentUser)
                .actorUser(actor)
                .type(NotificationType.FOLLOW)
                .isRead(true)
                .readAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("recipient", "recipient", "recipient"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(newest, older), PageRequest.of(0, 20), 2));

        PagedResponse<NotificationDTO> result = notificationService.getNotifications(0, 20, true);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(newest.getId());
        assertThat(result.getContent().get(1).getId()).isEqualTo(older.getId());
    }

    @Test
    void createNotification_WhenValid_ShouldPersistAndReturnDto() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        User actor = User.builder()
                .id(actorId)
                .username("actor")
                .avatarUrl("https://example.com/actor.jpg")
                .build();
        User recipient = User.builder()
                .id(recipientId)
                .username("recipient")
                .build();

        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            return notification;
        });

        NotificationDTO result = notificationService.createNotification(
                NotificationType.POST_LIKE,
                actorId,
                recipientId,
                postId,
                null
        );

        assertThat(result.getType()).isEqualTo(NotificationType.POST_LIKE);
        assertThat(result.getActorId()).isEqualTo(actorId);
        assertThat(result.getActorUsername()).isEqualTo("actor");
        assertThat(result.getPostId()).isEqualTo(postId);
    }

    @Test
    void markAsRead_WhenUnread_ShouldSetReadAndReadAt() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("recipient").build();
        authenticateUser("recipient");
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .recipientUser(currentUser)
                .actorUser(User.builder().id(UUID.randomUUID()).username("actor").build())
                .type(NotificationType.FOLLOW)
                .isRead(false)
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("recipient", "recipient", "recipient"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_WhenAlreadyRead_ShouldBeIdempotentAndNoOp() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("recipient").build();
        authenticateUser("recipient");
        UUID notificationId = UUID.randomUUID();
        LocalDateTime previousReadAt = LocalDateTime.now().minusMinutes(5);

        Notification notification = Notification.builder()
                .id(notificationId)
                .recipientUser(currentUser)
                .actorUser(User.builder().id(UUID.randomUUID()).username("actor").build())
                .type(NotificationType.FOLLOW)
                .isRead(true)
                .readAt(previousReadAt)
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("recipient", "recipient", "recipient"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId);

        assertThat(notification.getReadAt()).isEqualTo(previousReadAt);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_WhenNotificationBelongsToOtherUser_ShouldThrow() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("recipient").build();
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        authenticateUser("recipient");
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .recipientUser(otherUser)
                .actorUser(User.builder().id(UUID.randomUUID()).username("actor").build())
                .type(NotificationType.FOLLOW)
                .isRead(false)
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("recipient", "recipient", "recipient"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not authorized to update this notification");
    }

    @Test
    void markAllAsRead_ShouldReturnUpdatedCount() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("recipient").build();
        authenticateUser("recipient");

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("recipient", "recipient", "recipient"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.markAllAsReadByRecipientUserId(currentUser.getId())).thenReturn(3);

        int updatedCount = notificationService.markAllAsRead();

        assertThat(updatedCount).isEqualTo(3);
        verify(notificationRepository).markAllAsReadByRecipientUserId(currentUser.getId());
    }
}
