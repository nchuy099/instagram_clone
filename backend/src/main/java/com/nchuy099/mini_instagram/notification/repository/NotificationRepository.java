package com.nchuy099.mini_instagram.notification.repository;

import com.nchuy099.mini_instagram.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    @Modifying
    @Query("""
            update Notification n
            set n.isRead = true, n.readAt = CURRENT_TIMESTAMP
            where n.recipientUser.id = :recipientUserId and n.isRead = false
            """)
    int markAllAsReadByRecipientUserId(@Param("recipientUserId") UUID recipientUserId);
}
