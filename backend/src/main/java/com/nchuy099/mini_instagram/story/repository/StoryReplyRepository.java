package com.nchuy099.mini_instagram.story.repository;

import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.entity.StoryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoryReplyRepository extends JpaRepository<StoryReply, UUID> {
    long countByStory(Story story);
}
