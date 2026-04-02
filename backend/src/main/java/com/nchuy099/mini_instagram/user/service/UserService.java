package com.nchuy099.mini_instagram.user.service;

import com.nchuy099.mini_instagram.user.dto.ProfileHeaderDTO;
import com.nchuy099.mini_instagram.user.dto.HomeSuggestionDTO;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public User getByUsername(String identifier) {
        return userRepository.findByUsernameOrEmailOrPhoneNumber(identifier, identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier));
    }

    @Transactional(readOnly = true)
    public List<HomeSuggestionDTO> getHomeSuggestions() {
        return List.of(
                HomeSuggestionDTO.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .username("leomessi")
                        .fullName("Leo Messi")
                        .avatarUrl("https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?auto=format&fit=crop&w=200&q=80")
                        .subtitle("Followed by yourfriends + 5 more")
                        .build(),
                HomeSuggestionDTO.builder()
                        .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .username("kylianmbappe")
                        .fullName("Kylian Mbappe")
                        .avatarUrl("https://images.unsplash.com/photo-1552374196-c4e7ffc6e126?auto=format&fit=crop&w=200&q=80")
                        .subtitle("Suggested for you")
                        .build(),
                HomeSuggestionDTO.builder()
                        .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                        .username("alexzverev123")
                        .fullName("Alexander Zverev")
                        .avatarUrl("https://images.unsplash.com/photo-1521412644187-c49fa049e84d?auto=format&fit=crop&w=200&q=80")
                        .subtitle("Followed by tennis_hub")
                        .build(),
                HomeSuggestionDTO.builder()
                        .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                        .username("allianzarena")
                        .fullName("Allianz Arena")
                        .avatarUrl("https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=200&q=80")
                        .subtitle("Suggested for you")
                        .build()
        );
    }

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
        return userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential).orElse(null);
    }
}
