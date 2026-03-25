package com.nchuy099.mini_instagram.story.service;

import com.nchuy099.mini_instagram.story.dto.StoryDTO;
import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.repository.StoryRepository;
import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final FollowRepository followRepository;
    private final UserService userService;

    @Override
    @Transactional
    public StoryDTO createStory(String mediaUrl, String mediaType) {
        User currentUser = getCurrentUser();
        Story story = Story.builder()
                .user(currentUser)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .expiresAt(ZonedDateTime.now().plusHours(24))
                .build();
        
        Story savedStory = storyRepository.save(story);
        return mapToDTO(savedStory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDTO> getFollowingStories() {
        User currentUser = getCurrentUser();
        
        // Find users that current user follows
        List<Follow> following = followRepository.findAll().stream()
                .filter(f -> f.getFollower().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
        
        List<User> followedUsers = following.stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toCollection(ArrayList::new));
        
        // Add self to show own active stories in the bar
        followedUsers.add(currentUser);
        
        List<Story> activeStories = storyRepository.findByUserInAndExpiresAtAfterOrderByCreatedAtDesc(
                followedUsers, ZonedDateTime.now());
        
        return activeStories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<StoryDTO>> getGroupedStories() {
        List<StoryDTO> stories = getFollowingStories();
        return stories.stream()
                .collect(Collectors.groupingBy(StoryDTO::getUsername));
    }

    private StoryDTO mapToDTO(Story story) {
        return StoryDTO.builder()
                .id(story.getId())
                .userId(story.getUser().getId())
                .username(story.getUser().getUsername())
                .userAvatarUrl(story.getUser().getAvatarUrl())
                .mediaUrl(story.getMediaUrl())
                .mediaType(story.getMediaType())
                .createdAt(story.getCreatedAt() != null ? story.getCreatedAt().atZone(java.time.ZoneId.systemDefault()) : null)
                .expiresAt(story.getExpiresAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.getByUsername(auth.getName());
    }
}
