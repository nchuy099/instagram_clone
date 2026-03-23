package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getUserProfile_WhenUserExists_ShouldReturnProfile() {
        String targetUsername = "targetuser";
        User targetUser = User.builder()
                .id(UUID.randomUUID())
                .username(targetUsername)
                .fullName("Target Full Name")
                .followerCount(10)
                .build();

        when(userRepository.findByUsername(targetUsername)).thenReturn(Optional.of(targetUser));
        
        // Scenario 1: Unauthenticated
        SecurityContextHolder.clearContext();
        ProfileHeaderDTO profileAnon = userService.getUserProfile(targetUsername);
        assertThat(profileAnon.getUsername()).isEqualTo(targetUsername);
        assertThat(profileAnon.getIsFollowing()).isFalse();
        assertThat(profileAnon.getIsOwnProfile()).isFalse();

        // Scenario 2: Authenticated, looking at others
        authenticateUser("currentuser");
        User currentUser = User.builder().id(UUID.randomUUID()).username("currentuser").build();
        when(userRepository.findByUsernameOrEmailOrPhoneNumber("currentuser", "currentuser", "currentuser")).thenReturn(Optional.of(currentUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId())).thenReturn(true);
        
        ProfileHeaderDTO profileAuth = userService.getUserProfile(targetUsername);
        assertThat(profileAuth.getIsFollowing()).isTrue();
        assertThat(profileAuth.getIsOwnProfile()).isFalse();

        // Scenario 3: Looking at own profile
        authenticateUser(targetUsername);
        when(userRepository.findByUsernameOrEmailOrPhoneNumber(targetUsername, targetUsername, targetUsername)).thenReturn(Optional.of(targetUser));
        
        ProfileHeaderDTO ownProfile = userService.getUserProfile(targetUsername);
        assertThat(ownProfile.getIsOwnProfile()).isTrue();
    }

    @Test
    void getUserProfile_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findByUsername("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateProfile_WhenAuthenticated_ShouldUpdateFields() {
        authenticateUser("me");
        User me = User.builder().id(UUID.randomUUID()).username("me").build();
        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(me));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setBio("Updated Bio");

        UserDTO result = userService.updateProfile(request);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(me.getBio()).isEqualTo("Updated Bio");
        verify(userRepository).save(me);
    }

    @Test
    void updateProfile_WhenUnauthenticated_ShouldThrowException() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> userService.updateProfile(new UpdateProfileRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authentication required to update profile");
    }

    @Test
    void updateUsername_WhenTargetTaken_ShouldThrowException() {
        authenticateUser("me");
        User me = User.builder().id(UUID.randomUUID()).username("me").build();
        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(me));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUsername("taken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken");
    }
}
