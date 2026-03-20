package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.UpdateProfileRequest;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public ProfileHeaderDTO getUserProfile(String username) {
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isOwnProfile = false;
        boolean isFollowing = false;

        if (currentUser != null) {
            isOwnProfile = currentUser.getId().equals(targetUser.getId());
            if (!isOwnProfile) {
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());
            }
        }

        return ProfileHeaderDTO.builder()
                .id(targetUser.getId())
                .username(targetUser.getUsername())
                .fullName(targetUser.getFullName())
                .bio(targetUser.getBio())
                .avatarUrl(targetUser.getAvatarUrl())
                .postCount(targetUser.getPostCount())
                .followerCount(targetUser.getFollowerCount())
                .followingCount(targetUser.getFollowingCount())
                .isFollowing(isFollowing)
                .isOwnProfile(isOwnProfile)
                .build();
    }

    @Transactional
    public UserDTO updateProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Authentication required to update profile");
        }

        if (request.getFullName() != null) currentUser.setFullName(request.getFullName());
        if (request.getBio() != null) currentUser.setBio(request.getBio());
        if (request.getAvatarUrl() != null) currentUser.setAvatarUrl(request.getAvatarUrl());
        if (request.getWebsiteUrl() != null) currentUser.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getIsPrivate() != null) currentUser.setPrivate(request.getIsPrivate());

        User updatedUser = userRepository.save(currentUser);
        
        return UserDTO.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .avatarUrl(updatedUser.getAvatarUrl())
                .bio(updatedUser.getBio())
                .build();
    }

    @Transactional
    public UserDTO updateUsername(String newUsername) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Authentication required to update username");
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        currentUser.setUsername(newUsername);
        currentUser.setUsernameSet(true);
        
        User updatedUser = userRepository.save(currentUser);

        return UserDTO.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .avatarUrl(updatedUser.getAvatarUrl())
                .bio(updatedUser.getBio())
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String credential = authentication.getName();
        return userRepository.findByUsernameOrEmail(credential, credential).orElse(null);
    }
}
