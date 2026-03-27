package com.nchuy099.mini_instagram.story.repository;

import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.entity.StoryShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoryShareRepository extends JpaRepository<StoryShare, UUID> {
    long countByStory(Story story);
}
