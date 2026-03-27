package com.nchuy099.mini_instagram.message.repository;

import com.nchuy099.mini_instagram.message.entity.Conversation;
import com.nchuy099.mini_instagram.message.entity.ConversationParticipant;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    List<ConversationParticipant> findByUserOrderByConversationUpdatedAtDesc(User user);
    List<ConversationParticipant> findByConversation(Conversation conversation);
    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);
    boolean existsByConversationAndUser(Conversation conversation, User user);
}
