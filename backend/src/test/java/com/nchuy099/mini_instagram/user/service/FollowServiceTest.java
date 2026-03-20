package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private FollowService followService;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("currentuser").password("").authorities("USER").build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFollowUserAndIncrementCounter() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setUsername("currentuser");
        currentUser.setFollowingCount(0);

        UUID targetUserId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setUsername("targetuser");
        targetUser.setFollowerCount(0);

        when(userRepository.findByUsername("currentuser")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        followService.followUser(targetUserId);

        verify(followRepository).save(any(Follow.class));
        assertThat(currentUser.getFollowingCount()).isEqualTo(1);
        assertThat(targetUser.getFollowerCount()).isEqualTo(1);
        verify(userRepository, times(2)).save(any(User.class));
    }
}
