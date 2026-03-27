package com.nchuy099.mini_instagram.story.repository;

import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.entity.StoryLike;
import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryLikeRepository extends JpaRepository<StoryLike, UUID> {
    boolean existsByStoryAndUser(Story story, User user);
    Optional<StoryLike> findByStoryAndUser(Story story, User user);
    long countByStory(Story story);
}
