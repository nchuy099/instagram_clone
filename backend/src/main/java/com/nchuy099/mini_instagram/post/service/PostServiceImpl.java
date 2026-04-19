package com.nchuy099.mini_instagram.post.service;

import com.nchuy099.mini_instagram.post.dto.CreatePostRequest;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.dto.PostMediaDTO;
import com.nchuy099.mini_instagram.post.dto.UpdatePostRequest;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.entity.PostLike;
import com.nchuy099.mini_instagram.post.entity.PostMedia;
import com.nchuy099.mini_instagram.post.entity.PostSave;
import com.nchuy099.mini_instagram.post.entity.Hashtag;
import com.nchuy099.mini_instagram.post.entity.PostHashtag;
import com.nchuy099.mini_instagram.post.repository.HashtagRepository;
import com.nchuy099.mini_instagram.post.repository.PostLikeRepository;
import com.nchuy099.mini_instagram.post.repository.PostHashtagRepository;
import com.nchuy099.mini_instagram.post.repository.PostMediaRepository;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.post.repository.PostSaveRepository;
import com.nchuy099.mini_instagram.user.repository.FollowRepository;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9_]{1,100})");

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Override
    @Transactional
    public PostDTO createPost(CreatePostRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        Post post = Post.builder()
                .caption(request.getCaption())
                .location(request.getLocation())
                .user(currentUser)
                .allowComments(request.isAllowComments())
                .build();

        Post savedPost = postRepository.save(post);

        List<PostMedia> mediaList = request.getMedia().stream()
                .map(m -> PostMedia.builder()
                        .url(m.getUrl())
                        .thumbnailUrl(m.getThumbnailUrl())
                        .type(PostMedia.MediaType.valueOf(m.getType()))
                        .orderIndex(m.getOrderIndex())
                        .post(savedPost)
                        .build())
                .collect(Collectors.toList());

        postMediaRepository.saveAll(mediaList);
        savedPost.setMedia(mediaList);

        savePostHashtags(savedPost, request.getCaption());

        // Update user post count
        currentUser.setPostCount(currentUser.getPostCount() + 1);
        userRepository.save(currentUser);

        return mapToDTO(savedPost, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDTO getPostById(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUserQuietly();
        return mapToDTO(post, currentUser);
    }

    @Override
    @Transactional
    public PostDTO updatePost(UUID postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to update this post");
        }

        if (request.getCaption() != null) post.setCaption(request.getCaption());
        if (request.getLocation() != null) post.setLocation(request.getLocation());
        post.setAllowComments(request.isAllowComments());

        return mapToDTO(postRepository.save(post), currentUser);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this post");
        }

        postRepository.delete(post);

        // Update user post count
        currentUser.setPostCount(Math.max(0, currentUser.getPostCount() - 1));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void likePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        if (!postLikeRepository.existsByPostAndUser(post, currentUser)) {
            PostLike like = PostLike.builder()
                    .post(post)
                    .user(currentUser)
                    .build();
            postLikeRepository.save(like);

            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
        }
    }

    @Override
    @Transactional
    public void unlikePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        postLikeRepository.findByPostAndUser(post, currentUser).ifPresent(like -> {
            postLikeRepository.delete(like);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
        });
    }

    @Override
    @Transactional
    public void savePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        if (!postSaveRepository.existsByPostAndUser(post, currentUser)) {
            PostSave save = PostSave.builder()
                    .post(post)
                    .user(currentUser)
                    .build();
            postSaveRepository.save(save);
        }
    }

    @Override
    @Transactional
    public void unsavePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        User currentUser = getCurrentAuthenticatedUser();

        postSaveRepository.findByPostAndUser(post, currentUser).ifPresent(postSaveRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostDTO> getFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        User currentUser = getCurrentAuthenticatedUserQuietly();
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        return PagedResponse.<PostDTO>builder()
                .content(postPage.getContent().stream()
                        .map(post -> mapToDTO(post, currentUser))
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
    public PagedResponse<PostDTO> getUserPosts(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size);
        User currentUser = getCurrentAuthenticatedUserQuietly();
        Page<Post> postPage = postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        return PagedResponse.<PostDTO>builder()
                .content(postPage.getContent().stream()
                        .map(post -> mapToDTO(post, currentUser))
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
    public PagedResponse<PostDTO> getSavedPosts(int page, int size) {
        User currentUser = getCurrentAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<PostSave> savePage = postSaveRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        
        return PagedResponse.<PostDTO>builder()
                .content(savePage.getContent().stream()
                        .map(save -> mapToDTO(save.getPost(), currentUser))
                        .collect(Collectors.toList()))
                .pageNumber(savePage.getNumber())
                .pageSize(savePage.getSize())
                .totalElements(savePage.getTotalElements())
                .totalPages(savePage.getTotalPages())
                .last(savePage.isLast())
                .build();
    }

    private PostDTO mapToDTO(Post post, User currentUser) {
        User author = post.getUser();
        UserDTO userDTO = UserDTO.builder()
                .id(author.getId())
                .username(author.getUsername())
                .fullName(author.getFullName())
                .avatarUrl(author.getAvatarUrl())
                .bio(author.getBio())
                .build();

        List<PostMediaDTO> mediaDTOs = post.getMedia().stream()
                .map(m -> PostMediaDTO.builder()
                        .id(m.getId())
                        .url(m.getUrl())
                        .thumbnailUrl(m.getThumbnailUrl())
                        .type(m.getType())
                        .orderIndex(m.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        boolean isLiked = false;
        boolean isSaved = false;
        boolean isFollowing = false;
        if (currentUser != null) {
            isLiked = postLikeRepository.existsByPostAndUser(post, currentUser);
            isSaved = postSaveRepository.existsByPostAndUser(post, currentUser);
            if (!currentUser.getId().equals(author.getId())) {
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), author.getId());
            }
        }

        return PostDTO.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .location(post.getLocation())
                .user(userDTO)
                .media(mediaDTOs)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .allowComments(post.isAllowComments())
                .createdAt(post.getCreatedAt())
                .liked(isLiked)
                .saved(isSaved)
                .following(isFollowing)
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        User user = getCurrentAuthenticatedUserQuietly();
        if (user == null) {
            throw new IllegalStateException("Authentication required");
        }
        return user;
    }

    private User getCurrentAuthenticatedUserQuietly() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String credential = authentication.getName();
        return userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential).orElse(null);
    }

    private void savePostHashtags(Post post, String caption) {
        Set<String> hashtagNames = extractHashtags(caption);
        if (hashtagNames.isEmpty()) {
            return;
        }

        List<PostHashtag> mappings = hashtagNames.stream()
                .map(tagName -> {
                    Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(tagName)
                            .orElseGet(() -> hashtagRepository.save(Hashtag.builder().name(tagName).build()));

                    PostHashtag mapping = PostHashtag.builder()
                            .post(post)
                            .hashtag(hashtag)
                            .build();
                    return mapping;
                })
                .toList();

        postHashtagRepository.saveAll(mappings);
    }

    private Set<String> extractHashtags(String caption) {
        if (caption == null || caption.isBlank()) {
            return Set.of();
        }

        Matcher matcher = HASHTAG_PATTERN.matcher(caption);
        Set<String> tags = new LinkedHashSet<>();
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tags;
    }
}
