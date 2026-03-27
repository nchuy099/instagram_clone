package com.nchuy099.mini_instagram.story.service;

import com.nchuy099.mini_instagram.story.dto.StoryDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StoryService {
    StoryDTO createStory(String mediaUrl, String mediaType);
    List<StoryDTO> getFollowingStories();
    Map<String, List<StoryDTO>> getGroupedStories();
    StoryDTO likeStory(UUID storyId);
    StoryDTO unlikeStory(UUID storyId);
    StoryDTO replyToStory(UUID storyId, String content);
    StoryDTO shareStory(UUID storyId);
}
