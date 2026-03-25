package com.nchuy099.mini_instagram.feed.service;

import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.post.service.PostService;
import com.nchuy099.mini_instagram.user.entity.Follow;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final UserService userService;
    private final PostService postService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostDTO> getFollowingFeed(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        
        // Find users that current user follows
        // This is a simple implementation for MVP
        List<Follow> following = followRepository.findAll().stream()
                .filter(f -> f.getFollower().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
        
        List<User> followedUsers = following.stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toCollection(ArrayList::new));
        
        // Include self
        followedUsers.add(currentUser);
        
        Page<Post> postPage = postRepository.findByUserInOrderByCreatedAtDesc(followedUsers, pageable);
        
        return PagedResponse.<PostDTO>builder()
                .content(postPage.getContent().stream()
                        .map(post -> postService.getPostById(post.getId())) // This might be slow if many posts, but getPostById handles mapping
                        .collect(Collectors.toList()))
                .pageNumber(postPage.getNumber())
                .pageSize(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .last(postPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostDTO> getExploreFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        User currentUser = getCurrentUser();
        
        return PagedResponse.<PostDTO>builder()
                .content(postPage.getContent().stream()
                        .map(post -> postService.getPostById(post.getId()))
                        .collect(Collectors.toList()))
                .pageNumber(postPage.getNumber())
                .pageSize(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .last(postPage.isLast())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.getByUsername(auth.getName());
    }
}
