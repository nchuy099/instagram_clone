package com.nchuy099.mini_instagram.story.service;

import com.nchuy099.mini_instagram.story.dto.StoryDTO;
import java.util.List;
import java.util.Map;

public interface StoryService {
    StoryDTO createStory(String mediaUrl, String mediaType);
    List<StoryDTO> getFollowingStories();
    // Stories grouped by user for the horizontal bar
    Map<String, List<StoryDTO>> getGroupedStories();
}
