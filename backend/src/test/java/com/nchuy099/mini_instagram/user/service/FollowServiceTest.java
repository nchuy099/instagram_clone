package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.notification.event.UserFollowedEvent;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private FollowService followService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void followUser_WhenValid_ShouldCreateFollow() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").followingCount(0).build();
        authenticateUser("me");

        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.builder().id(targetUserId).username("target").followerCount(0).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        followService.followUser(targetUserId);

        verify(followRepository).save(any(Follow.class));
        assertThat(currentUser.getFollowingCount()).isEqualTo(1);
        assertThat(targetUser.getFollowerCount()).isEqualTo(1);
        ArgumentCaptor<UserFollowedEvent> eventCaptor = ArgumentCaptor.forClass(UserFollowedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActorId()).isEqualTo(currentUserId);
        assertThat(eventCaptor.getValue().getRecipientId()).isEqualTo(targetUserId);
        assertThat(eventCaptor.getValue().getRecipientPrincipal()).isEqualTo("target");
    }

    @Test
    void followUser_WhenAuthenticatedWithEmail_ShouldSucceed() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).email("me@example.com").followingCount(0).build();
        authenticateUser("me@example.com");

        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.builder().id(targetUserId).username("target").followerCount(0).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me@example.com", "me@example.com", "me@example.com"))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        followService.followUser(targetUserId);

        verify(followRepository).save(any(Follow.class));
        assertThat(currentUser.getFollowingCount()).isEqualTo(1);
        assertThat(targetUser.getFollowerCount()).isEqualTo(1);
        verify(applicationEventPublisher).publishEvent(any(UserFollowedEvent.class));
    }

    @Test
    void followUser_WhenAlreadyFollowing_ShouldNotPublishEvent() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").followingCount(1).build();
        authenticateUser("me");

        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.builder().id(targetUserId).username("target").followerCount(1).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        followService.followUser(targetUserId);

        verify(followRepository, never()).save(any(Follow.class));
        verify(applicationEventPublisher, never()).publishEvent(any(UserFollowedEvent.class));
    }

    @Test
    void followUser_WhenSelf_ShouldThrowException() {
        UUID myId = UUID.randomUUID();
        User me = User.builder().id(myId).username("me").build();
        authenticateUser("me");

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> followService.followUser(myId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot follow yourself");
    }

    @Test
    void unfollowUser_WhenExists_ShouldRemoveFollow() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").followingCount(1).build();
        authenticateUser("me");

        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.builder().id(targetUserId).username("target").followerCount(1).build();

        Follow follow = Follow.builder().follower(currentUser).following(targetUser).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(Optional.of(follow));

        followService.unfollowUser(targetUserId);

        verify(followRepository).delete(follow);
        assertThat(currentUser.getFollowingCount()).isEqualTo(0);
        assertThat(targetUser.getFollowerCount()).isEqualTo(0);
    }

    @Test
    void getFollowers_ShouldReturnPageOfDTOs() {
        String username = "user1";
        Pageable pageable = PageRequest.of(0, 10);
        User follower = User.builder().id(UUID.randomUUID()).username("follower").build();
        Follow follow = Follow.builder().follower(follower).build();
        Page<Follow> followPage = new PageImpl<>(List.of(follow));

        when(followRepository.findByFollowingUsername(username, pageable)).thenReturn(followPage);
 
        PagedResponse<UserDTO> result = followService.getFollowers(username, pageable);
 
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("follower");
    }

    @Test
    void getFollowing_ShouldReturnPageOfDTOs() {
        String username = "user1";
        Pageable pageable = PageRequest.of(0, 10);
        User following = User.builder().id(UUID.randomUUID()).username("following").build();
        Follow follow = Follow.builder().following(following).build();
        Page<Follow> followPage = new PageImpl<>(List.of(follow));

        when(followRepository.findByFollowerUsername(username, pageable)).thenReturn(followPage);
 
        PagedResponse<UserDTO> result = followService.getFollowing(username, pageable);
 
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("following");
    }

    @Test
    void getFollowingForMessageSearch_ShouldMergeFollowingAndFollowersWithoutDuplicates() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").build();
        authenticateUser("me");

        User followingOnly = User.builder()
                .id(UUID.randomUUID())
                .username("following-only")
                .fullName("Following Only")
                .build();
        User shared = User.builder()
                .id(UUID.randomUUID())
                .username("shared")
                .fullName("Shared User")
                .build();
        User followerOnly = User.builder()
                .id(UUID.randomUUID())
                .username("follower-only")
                .fullName("Follower Only")
                .build();
        User self = User.builder().id(currentUserId).username("me").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(followRepository.findFollowingForMessageSearch(eq(currentUserId), eq("alice"), any(Pageable.class)))
                .thenReturn(List.of(
                        Follow.builder().following(followingOnly).build(),
                        Follow.builder().following(shared).build()
                ));
        when(followRepository.findFollowersForMessageSearch(eq(currentUserId), eq("alice"), any(Pageable.class)))
                .thenReturn(List.of(
                        Follow.builder().follower(shared).build(),
                        Follow.builder().follower(self).build(),
                        Follow.builder().follower(followerOnly).build()
                ));

        List<UserDTO> result = followService.getFollowingForMessageSearch("  alice  ", 3);

        assertThat(result).extracting(UserDTO::getUsername)
                .containsExactly("following-only", "shared", "follower-only");
        verify(followRepository).findFollowingForMessageSearch(eq(currentUserId), eq("alice"), any(Pageable.class));
        verify(followRepository).findFollowersForMessageSearch(eq(currentUserId), eq("alice"), any(Pageable.class));
    }

    @Test
    void getFollowingForMessageSearch_ShouldRespectLimitAfterDeduplication() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").build();
        authenticateUser("me");

        User first = User.builder().id(UUID.randomUUID()).username("first").build();
        User second = User.builder().id(UUID.randomUUID()).username("second").build();
        User third = User.builder().id(UUID.randomUUID()).username("third").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(followRepository.findFollowingForMessageSearch(eq(currentUserId), eq(""), any(Pageable.class)))
                .thenReturn(List.of(
                        Follow.builder().following(first).build(),
                        Follow.builder().following(second).build()
                ));
        when(followRepository.findFollowersForMessageSearch(eq(currentUserId), eq(""), any(Pageable.class)))
                .thenReturn(List.of(
                        Follow.builder().follower(second).build(),
                        Follow.builder().follower(third).build()
                ));

        List<UserDTO> result = followService.getFollowingForMessageSearch(null, 2);

        assertThat(result).extracting(UserDTO::getUsername)
                .containsExactly("first", "second");
        assertThat(result).hasSize(2);
    }
}
