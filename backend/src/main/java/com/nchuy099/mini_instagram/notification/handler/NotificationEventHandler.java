package com.nchuy099.mini_instagram.notification.handler;

import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.dto.NotificationEventDTO;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import com.nchuy099.mini_instagram.notification.event.CommentCreatedEvent;
import com.nchuy099.mini_instagram.notification.event.PostLikedEvent;
import com.nchuy099.mini_instagram.notification.event.UserFollowedEvent;
import com.nchuy099.mini_instagram.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {

    private static final String USER_QUEUE_DESTINATION = "/queue/notifications";
    private static final String NOTIFICATION_CREATED_EVENT = "NOTIFICATION_CREATED";

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handlePostLikedEvent(PostLikedEvent event) {
        log.info(
                "notification_event_received type=POST_LIKE actorId={} recipientId={} postId={}",
                event.getActorId(),
                event.getRecipientId(),
                event.getPostId()
        );
        if (isSelfAction(event.getActorId(), event.getRecipientId())) {
            log.info(
                    "notification_event_skipped_self_action type=POST_LIKE actorId={} recipientId={}",
                    event.getActorId(),
                    event.getRecipientId()
            );
            return;
        }

        NotificationDTO notification = notificationService.createNotification(
                NotificationType.POST_LIKE,
                event.getActorId(),
                event.getRecipientId(),
                event.getPostId(),
                null
        );
        publish(event.getRecipientPrincipal(), notification);
    }

    @EventListener
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        log.info(
                "notification_event_received type=POST_COMMENT actorId={} recipientId={} postId={} commentId={}",
                event.getActorId(),
                event.getRecipientId(),
                event.getPostId(),
                event.getCommentId()
        );
        if (isSelfAction(event.getActorId(), event.getRecipientId())) {
            log.info(
                    "notification_event_skipped_self_action type=POST_COMMENT actorId={} recipientId={}",
                    event.getActorId(),
                    event.getRecipientId()
            );
            return;
        }

        NotificationDTO notification = notificationService.createNotification(
                NotificationType.POST_COMMENT,
                event.getActorId(),
                event.getRecipientId(),
                event.getPostId(),
                event.getCommentId()
        );
        publish(event.getRecipientPrincipal(), notification);
    }

    @EventListener
    public void handleUserFollowedEvent(UserFollowedEvent event) {
        log.info(
                "notification_event_received type=FOLLOW actorId={} recipientId={}",
                event.getActorId(),
                event.getRecipientId()
        );
        if (isSelfAction(event.getActorId(), event.getRecipientId())) {
            log.info(
                    "notification_event_skipped_self_action type=FOLLOW actorId={} recipientId={}",
                    event.getActorId(),
                    event.getRecipientId()
            );
            return;
        }

        NotificationDTO notification = notificationService.createNotification(
                NotificationType.FOLLOW,
                event.getActorId(),
                event.getRecipientId(),
                null,
                null
        );
        publish(event.getRecipientPrincipal(), notification);
    }

    private boolean isSelfAction(java.util.UUID actorId, java.util.UUID recipientId) {
        return actorId != null && actorId.equals(recipientId);
    }

    private void publish(String recipientPrincipal, NotificationDTO notification) {
        NotificationEventDTO payload = NotificationEventDTO.builder()
                .type(NOTIFICATION_CREATED_EVENT)
                .notification(notification)
                .build();
        log.info(
                "notification_websocket_push destination={} recipientPrincipal={} notificationId={} type={}",
                USER_QUEUE_DESTINATION,
                recipientPrincipal,
                notification.getId(),
                notification.getType()
        );
        messagingTemplate.convertAndSendToUser(recipientPrincipal, USER_QUEUE_DESTINATION, payload);
    }
}
