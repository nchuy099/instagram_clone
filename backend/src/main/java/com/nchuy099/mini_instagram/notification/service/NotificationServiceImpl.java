package com.nchuy099.mini_instagram.notification.service;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.entity.Notification;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import com.nchuy099.mini_instagram.notification.repository.NotificationRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationDTO> getNotifications(int page, int size, boolean unreadOnly) {
        User currentUser = getCurrentAuthenticatedUser();
        log.info("notification_list_start userId={} page={} size={} unreadOnly={}", currentUser.getId(), page, size, unreadOnly);
        Page<Notification> notificationPage = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(
                        currentUser.getId(),
                        PageRequest.of(page, size)
                )
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        PageRequest.of(page, size)
                );
        log.info(
                "notification_list_loaded userId={} items={} totalElements={} totalPages={} unreadOnly={}",
                currentUser.getId(),
                notificationPage.getNumberOfElements(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                unreadOnly
        );

        return PagedResponse.<NotificationDTO>builder()
                .content(notificationPage.getContent().stream()
                        .map(this::mapToDTO)
                        .toList())
                .pageNumber(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        User currentUser = getCurrentAuthenticatedUser();
        log.info("notification_mark_read_start notificationId={} requesterId={}", notificationId, currentUser.getId());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getRecipientUser().getId().equals(currentUser.getId())) {
            log.warn(
                    "notification_mark_read_forbidden notificationId={} requesterId={} ownerId={}",
                    notificationId,
                    currentUser.getId(),
                    notification.getRecipientUser().getId()
            );
            throw new IllegalStateException("Not authorized to update this notification");
        }

        if (notification.isRead()) {
            log.info("notification_mark_read_noop notificationId={} requesterId={}", notificationId, currentUser.getId());
            return;
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("notification_mark_read_success notificationId={} requesterId={}", notificationId, currentUser.getId());
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        User currentUser = getCurrentAuthenticatedUser();
        log.info("notification_mark_all_read_start requesterId={}", currentUser.getId());
        int updatedCount = notificationRepository.markAllAsReadByRecipientUserId(currentUser.getId());
        log.info("notification_mark_all_read_success requesterId={} updatedCount={}", currentUser.getId(), updatedCount);
        return updatedCount;
    }

    @Override
    @Transactional
    public NotificationDTO createNotification(
            NotificationType type,
            UUID actorId,
            UUID recipientId,
            UUID postId,
            UUID commentId
    ) {
        log.info(
                "notification_create_start type={} actorId={} recipientId={} postId={} commentId={}",
                type,
                actorId,
                recipientId,
                postId,
                commentId
        );
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor user not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found"));

        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .actorUser(actor)
                .type(type)
                .postId(postId)
                .commentId(commentId)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info(
                "notification_create_success notificationId={} type={} actorId={} recipientId={}",
                savedNotification.getId(),
                savedNotification.getType(),
                actorId,
                recipientId
        );
        return mapToDTO(savedNotification);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        User actor = notification.getActorUser();
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .actorId(actor.getId())
                .actorUsername(actor.getUsername())
                .actorAvatarUrl(actor.getAvatarUrl())
                .postId(notification.getPostId())
                .commentId(notification.getCommentId())
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Authentication required");
        }

        String credential = authentication.getName();
        return userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}
