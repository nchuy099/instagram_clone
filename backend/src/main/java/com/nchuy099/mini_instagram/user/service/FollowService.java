package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public PagedResponse<UserDTO> getFollowers(String username, Pageable pageable) {
        Page<UserDTO> page = followRepository.findByFollowingUsername(username, pageable)
                .map(follow -> UserDTO.builder()
                        .id(follow.getFollower().getId())
                        .username(follow.getFollower().getUsername())
                        .fullName(follow.getFollower().getFullName())
                        .avatarUrl(follow.getFollower().getAvatarUrl())
                        .bio(follow.getFollower().getBio())
                        .build());
        return PagedResponse.fromPage(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDTO> getFollowing(String username, Pageable pageable) {
        Page<UserDTO> page = followRepository.findByFollowerUsername(username, pageable)
                .map(follow -> UserDTO.builder()
                        .id(follow.getFollowing().getId())
                        .username(follow.getFollowing().getUsername())
                        .fullName(follow.getFollowing().getFullName())
                        .avatarUrl(follow.getFollowing().getAvatarUrl())
                        .bio(follow.getFollowing().getBio())
                        .build());
        return PagedResponse.fromPage(page);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getFollowingForMessageSearch(String query, int limit) {
        User currentUser = getCurrentAuthenticatedUser();
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        String normalizedQuery = query == null ? "" : query.trim();
        Pageable searchPageable = PageRequest.of(0, normalizedLimit);

        Map<UUID, UserDTO> uniqueCandidates = new LinkedHashMap<>();

        followRepository.findFollowingForMessageSearch(
                        currentUser.getId(),
                        normalizedQuery,
                        searchPageable
                )
                .stream()
                .map(Follow::getFollowing)
                .forEach(user -> addCandidate(uniqueCandidates, user, currentUser.getId()));

        followRepository.findFollowersForMessageSearch(
                        currentUser.getId(),
                        normalizedQuery,
                        searchPageable
                )
                .stream()
                .map(Follow::getFollower)
                .forEach(user -> addCandidate(uniqueCandidates, user, currentUser.getId()));

        return uniqueCandidates.values().stream()
                .limit(normalizedLimit)
                .toList();
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

        String credential = authentication.getName();
        return userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    private void addCandidate(Map<UUID, UserDTO> uniqueCandidates, User user, UUID currentUserId) {
        if (user == null || user.getId() == null || user.getId().equals(currentUserId)) {
            return;
        }

        uniqueCandidates.putIfAbsent(user.getId(), UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build());
    }
}
