package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public void followUser(UUID targetUserId) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (!followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId())) {
            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            followRepository.save(follow);

            currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);
            targetUser.setFollowerCount(targetUser.getFollowerCount() + 1);
            userRepository.save(currentUser);
            userRepository.save(targetUser);
        }
    }

    @Transactional
    public void unfollowUser(UUID targetUserId) {
        User currentUser = getCurrentAuthenticatedUser();
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId())
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    
                    currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));
                    targetUser.setFollowerCount(Math.max(0, targetUser.getFollowerCount() - 1));
                    userRepository.save(currentUser);
                    userRepository.save(targetUser);
                });
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Authentication required");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}
