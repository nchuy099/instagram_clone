package com.nchuy099.mini_instagram.message.repository;

import com.nchuy099.mini_instagram.message.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query(value = """
            select cp1.conversation_id
            from conversation_participants cp1
            join conversation_participants cp2 on cp1.conversation_id = cp2.conversation_id
            where cp1.user_id = :userId and cp2.user_id = :otherUserId
            limit 1
            """, nativeQuery = true)
    Optional<UUID> findDirectConversationId(UUID userId, UUID otherUserId);
}
