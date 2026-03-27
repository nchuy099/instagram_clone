package com.nchuy099.mini_instagram.story.repository;

import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByUserInAndExpiresAtAfterOrderByCreatedAtDesc(List<User> users, ZonedDateTime now);
    List<Story> findByUserAndExpiresAtAfterOrderByCreatedAtDesc(User user, ZonedDateTime now);
    Optional<Story> findByIdAndExpiresAtAfter(UUID id, ZonedDateTime now);
}
