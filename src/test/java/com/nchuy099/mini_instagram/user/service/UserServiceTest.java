package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
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
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserService userService;

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
    void shouldReturnProfileWithFollowStatus() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("currentuser");

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("targetuser");
        targetUser.setFollowerCount(10);

        when(userRepository.findByUsername("currentuser")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        ProfileHeaderDTO profile = userService.getUserProfile("targetuser");

        assertThat(profile).isNotNull();
        assertThat(profile.getUsername()).isEqualTo("targetuser");
        assertThat(profile.getIsFollowing()).isTrue();
        assertThat(profile.getIsOwnProfile()).isFalse();
        assertThat(profile.getFollowerCount()).isEqualTo(10);
    }
}
