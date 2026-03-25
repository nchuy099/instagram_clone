package com.nchuy099.mini_instagram.post.service;

import com.nchuy099.mini_instagram.post.dto.CreatePostRequest;
import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.post.dto.UpdatePostRequest;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.entity.PostLike;
import com.nchuy099.mini_instagram.post.entity.PostSave;
import com.nchuy099.mini_instagram.post.entity.PostHashtag;
import com.nchuy099.mini_instagram.post.entity.Hashtag;
import com.nchuy099.mini_instagram.post.repository.PostLikeRepository;
import com.nchuy099.mini_instagram.post.repository.PostMediaRepository;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.post.repository.PostSaveRepository;
import com.nchuy099.mini_instagram.post.repository.PostHashtagRepository;
import com.nchuy099.mini_instagram.post.repository.HashtagRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostMediaRepository postMediaRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostSaveRepository postSaveRepository;
    @Mock
    private PostHashtagRepository postHashtagRepository;
    @Mock
    private HashtagRepository hashtagRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostServiceImpl postService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createPost_WhenValid_ShouldSucceed() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").postCount(0).build();
        authenticateUser("me");

        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("Hello world");
        
        CreatePostRequest.PostMediaRequest mediaRequest = new CreatePostRequest.PostMediaRequest();
        mediaRequest.setUrl("http://image.jpg");
        mediaRequest.setType("IMAGE");
        request.setMedia(Collections.singletonList(mediaRequest));

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PostDTO result = postService.createPost(request);

        assertThat(result).isNotNull();
        assertThat(result.getCaption()).isEqualTo("Hello world");
        verify(postMediaRepository).saveAll(any());
        verify(userRepository).save(currentUser);
        assertThat(currentUser.getPostCount()).isEqualTo(1);
    }

    @Test
    void createPost_WhenCaptionContainsHashtags_ShouldPersistPostHashtags() {
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").postCount(0).build();
        authenticateUser("me");

        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("This is #SpringBoot and #java");

        CreatePostRequest.PostMediaRequest mediaRequest = new CreatePostRequest.PostMediaRequest();
        mediaRequest.setUrl("http://image.jpg");
        mediaRequest.setType("IMAGE");
        request.setMedia(Collections.singletonList(mediaRequest));

        Hashtag springBoot = Hashtag.builder().id(UUID.randomUUID()).name("springboot").build();
        Hashtag java = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(hashtagRepository.findByNameIgnoreCase("springboot")).thenReturn(Optional.of(springBoot));
        when(hashtagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(java));

        postService.createPost(request);

        verify(postHashtagRepository).saveAll(any());
    }

    @Test
    void getPostById_WhenExists_ShouldReturnDTO() {
        UUID postId = UUID.randomUUID();
        User author = User.builder().id(UUID.randomUUID()).username("author").build();
        Post post = Post.builder().id(postId).user(author).caption("test").media(Collections.emptyList()).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostDTO result = postService.getPostById(postId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
    }

    @Test
    void updatePost_WhenOwner_ShouldSucceed() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).user(user).caption("old").media(Collections.emptyList()).build();
        UpdatePostRequest request = new UpdatePostRequest();
        request.setCaption("new");

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostDTO result = postService.updatePost(postId, request);

        assertThat(result.getCaption()).isEqualTo("new");
    }

    @Test
    void updatePost_WhenNotOwner_ShouldThrowException() {
        UUID postId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        User notOwner = User.builder().id(UUID.randomUUID()).username("notowner").build();
        authenticateUser("notowner");

        Post post = Post.builder().id(postId).user(owner).build();
        UpdatePostRequest request = new UpdatePostRequest();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("notowner", "notowner", "notowner")).thenReturn(Optional.of(notOwner));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(postId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are not authorized to update this post");
    }

    @Test
    void likePost_WhenNotLiked_ShouldCreateLike() {
        UUID postId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");
        Post post = Post.builder().id(postId).likeCount(0).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostAndUser(post, user)).thenReturn(false);

        postService.likePost(postId);

        verify(postLikeRepository).save(any(PostLike.class));
        assertThat(post.getLikeCount()).isEqualTo(1);
    }

    @Test
    void savePost_WhenNotSaved_ShouldCreateSave() {
        UUID postId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");
        Post post = Post.builder().id(postId).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postSaveRepository.existsByPostAndUser(post, user)).thenReturn(false);

        postService.savePost(postId);

        verify(postSaveRepository).save(any(PostSave.class));
    }
}
