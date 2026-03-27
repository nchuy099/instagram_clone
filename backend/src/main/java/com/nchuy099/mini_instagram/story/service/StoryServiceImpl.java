package com.nchuy099.mini_instagram.story.service;

import com.nchuy099.mini_instagram.story.dto.StoryDTO;
import com.nchuy099.mini_instagram.story.entity.Story;
import com.nchuy099.mini_instagram.story.entity.StoryLike;
import com.nchuy099.mini_instagram.story.entity.StoryReply;
import com.nchuy099.mini_instagram.story.entity.StoryShare;
import com.nchuy099.mini_instagram.story.repository.StoryLikeRepository;
import com.nchuy099.mini_instagram.story.repository.StoryReplyRepository;
import com.nchuy099.mini_instagram.story.repository.StoryRepository;
import com.nchuy099.mini_instagram.story.repository.StoryShareRepository;
import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final FollowRepository followRepository;
    private final UserService userService;
    private final StoryLikeRepository storyLikeRepository;
    private final StoryReplyRepository storyReplyRepository;
    private final StoryShareRepository storyShareRepository;

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
        return populateStoryMetrics(mapToDTO(savedStory), savedStory, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDTO> getFollowingStories() {
        User currentUser = getCurrentUser();

        List<Follow> following = followRepository.findAll().stream()
                .filter(f -> f.getFollower().getId().equals(currentUser.getId()))
                .toList();

        List<User> followedUsers = following.stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toCollection(ArrayList::new));

        followedUsers.add(currentUser);

        List<Story> activeStories = storyRepository.findByUserInAndExpiresAtAfterOrderByCreatedAtDesc(
                followedUsers, ZonedDateTime.now());

        return activeStories.stream()
                .map(story -> populateStoryMetrics(mapToDTO(story), story, currentUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<StoryDTO>> getGroupedStories() {
        List<StoryDTO> stories = getFollowingStories();
        return stories.stream()
                .collect(Collectors.groupingBy(StoryDTO::getUsername));
    }

    @Override
    @Transactional
    public StoryDTO likeStory(UUID storyId) {
        Story story = getActiveStoryById(storyId);
        User currentUser = getCurrentUser();

        if (!storyLikeRepository.existsByStoryAndUser(story, currentUser)) {
            storyLikeRepository.save(StoryLike.builder()
                    .story(story)
                    .user(currentUser)
                    .build());
        }

        return populateStoryMetrics(mapToDTO(story), story, currentUser);
    }

    @Override
    @Transactional
    public StoryDTO unlikeStory(UUID storyId) {
        Story story = getActiveStoryById(storyId);
        User currentUser = getCurrentUser();

        storyLikeRepository.findByStoryAndUser(story, currentUser)
                .ifPresent(storyLikeRepository::delete);

        return populateStoryMetrics(mapToDTO(story), story, currentUser);
    }

    @Override
    @Transactional
    public StoryDTO replyToStory(UUID storyId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Reply content is required");
        }

        Story story = getActiveStoryById(storyId);
        User currentUser = getCurrentUser();

        storyReplyRepository.save(StoryReply.builder()
                .story(story)
                .user(currentUser)
                .content(content.trim())
                .build());

        return populateStoryMetrics(mapToDTO(story), story, currentUser);
    }

    @Override
    @Transactional
    public StoryDTO shareStory(UUID storyId) {
        Story story = getActiveStoryById(storyId);
        User currentUser = getCurrentUser();

        storyShareRepository.save(StoryShare.builder()
                .story(story)
                .user(currentUser)
                .build());

        return populateStoryMetrics(mapToDTO(story), story, currentUser);
    }

    private Story getActiveStoryById(UUID storyId) {
        return storyRepository.findByIdAndExpiresAtAfter(storyId, ZonedDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Story not found or expired"));
    }

    private StoryDTO populateStoryMetrics(StoryDTO dto, Story story, User currentUser) {
        dto.setLikeCount(storyLikeRepository.countByStory(story));
        dto.setReplyCount(storyReplyRepository.countByStory(story));
        dto.setShareCount(storyShareRepository.countByStory(story));
        dto.setLikedByCurrentUser(storyLikeRepository.existsByStoryAndUser(story, currentUser));
        return dto;
    }

    private StoryDTO mapToDTO(Story story) {
        return StoryDTO.builder()
                .id(story.getId())
                .userId(story.getUser().getId())
                .username(story.getUser().getUsername())
                .userAvatarUrl(story.getUser().getAvatarUrl())
                .mediaUrl(story.getMediaUrl())
                .mediaType(story.getMediaType())
                .createdAt(story.getCreatedAt() != null ? story.getCreatedAt().atZone(ZoneId.systemDefault()) : null)
                .expiresAt(story.getExpiresAt())
                .likeCount(0L)
                .replyCount(0L)
                .shareCount(0L)
                .likedByCurrentUser(false)
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.getByUsername(auth.getName());
    }
}
