package com.nchuy099.mini_instagram.message.repository;

import com.nchuy099.mini_instagram.message.entity.Conversation;
import com.nchuy099.mini_instagram.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);

    @Query("""
            select count(m)
            from Message m
            where m.conversation = :conversation
              and m.user.id <> :userId
            """)
    long countUnreadMessagesWithoutLastRead(Conversation conversation, UUID userId);

    @Query("""
            select count(m)
            from Message m
            where m.conversation = :conversation
              and m.user.id <> :userId
              and m.createdAt > :lastReadAt
            """)
    long countUnreadMessagesSince(Conversation conversation, UUID userId, LocalDateTime lastReadAt);

    default long countUnreadMessages(Conversation conversation, UUID userId, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return countUnreadMessagesWithoutLastRead(conversation, userId);
        }
        return countUnreadMessagesSince(conversation, userId, lastReadAt);
    }
}
