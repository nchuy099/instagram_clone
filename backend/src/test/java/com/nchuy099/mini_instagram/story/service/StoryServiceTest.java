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
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserService userService;
    @Mock
    private StoryLikeRepository storyLikeRepository;
    @Mock
    private StoryReplyRepository storyReplyRepository;
    @Mock
    private StoryShareRepository storyShareRepository;

    @InjectMocks
    private StoryServiceImpl storyService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User authenticateAndGetCurrentUser() {
        String username = "me";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList())
        );
        User currentUser = User.builder().id(UUID.randomUUID()).username(username).build();
        when(userService.getByUsername(username)).thenReturn(currentUser);
        return currentUser;
    }

    private Story buildActiveStory() {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        return Story.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .mediaUrl("https://cdn/story.jpg")
                .mediaType("IMAGE")
                .expiresAt(ZonedDateTime.now().plusHours(12))
                .build();
    }

    private void mockCounters(Story story, User currentUser, boolean liked, long likes, long replies, long shares) {
        when(storyLikeRepository.existsByStoryAndUser(story, currentUser)).thenReturn(liked);
        when(storyLikeRepository.countByStory(story)).thenReturn(likes);
        when(storyReplyRepository.countByStory(story)).thenReturn(replies);
        when(storyShareRepository.countByStory(story)).thenReturn(shares);
    }

    @Test
    void likeStory_WhenNotLiked_ShouldCreateLikeAndReturnUpdatedDto() {
        User currentUser = authenticateAndGetCurrentUser();
        Story story = buildActiveStory();

        when(storyRepository.findByIdAndExpiresAtAfter(eq(story.getId()), any(ZonedDateTime.class))).thenReturn(Optional.of(story));
        when(storyLikeRepository.existsByStoryAndUser(story, currentUser)).thenReturn(false, true);
        when(storyLikeRepository.save(any(StoryLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storyLikeRepository.countByStory(story)).thenReturn(1L);
        when(storyReplyRepository.countByStory(story)).thenReturn(0L);
        when(storyShareRepository.countByStory(story)).thenReturn(0L);

        StoryDTO result = storyService.likeStory(story.getId());

        assertThat(result.getLikeCount()).isEqualTo(1);
        assertThat(result.getLikedByCurrentUser()).isTrue();
        verify(storyLikeRepository).save(any(StoryLike.class));
    }

    @Test
    void replyToStory_WhenValidContent_ShouldCreateReplyAndReturnUpdatedDto() {
        User currentUser = authenticateAndGetCurrentUser();
        Story story = buildActiveStory();

        when(storyRepository.findByIdAndExpiresAtAfter(eq(story.getId()), any(ZonedDateTime.class))).thenReturn(Optional.of(story));
        when(storyReplyRepository.save(any(StoryReply.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockCounters(story, currentUser, false, 0L, 1L, 0L);

        StoryDTO result = storyService.replyToStory(story.getId(), "Nice story!");

        assertThat(result.getReplyCount()).isEqualTo(1);
        verify(storyReplyRepository).save(any(StoryReply.class));
    }

    @Test
    void shareStory_WhenCalled_ShouldCreateShareAndReturnUpdatedDto() {
        User currentUser = authenticateAndGetCurrentUser();
        Story story = buildActiveStory();

        when(storyRepository.findByIdAndExpiresAtAfter(eq(story.getId()), any(ZonedDateTime.class))).thenReturn(Optional.of(story));
        when(storyShareRepository.save(any(StoryShare.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockCounters(story, currentUser, false, 0L, 0L, 1L);

        StoryDTO result = storyService.shareStory(story.getId());

        assertThat(result.getShareCount()).isEqualTo(1);
        verify(storyShareRepository).save(any(StoryShare.class));
    }
}
