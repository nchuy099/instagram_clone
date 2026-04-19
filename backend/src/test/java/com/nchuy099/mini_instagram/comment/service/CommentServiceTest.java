package com.nchuy099.mini_instagram.comment.service;

import com.nchuy099.mini_instagram.comment.dto.CommentDTO;
import com.nchuy099.mini_instagram.comment.dto.CreateCommentRequest;
import com.nchuy099.mini_instagram.comment.entity.Comment;
import com.nchuy099.mini_instagram.comment.repository.CommentRepository;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createComment_WhenValid_ShouldSucceed() {
        UUID postId = UUID.randomUUID();
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).allowComments(true).commentCount(0).build();
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Nice post!");

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        CommentDTO result = commentService.createComment(postId, request);

        assertThat(result.getContent()).isEqualTo("Nice post!");
        assertThat(post.getCommentCount()).isEqualTo(1);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_WhenCommentsDisabled_ShouldThrowException() {
        UUID postId = UUID.randomUUID();
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).allowComments(false).build();
        CreateCommentRequest request = new CreateCommentRequest();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(postId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Comments are disabled for this post");
    }

    @Test
    void deleteComment_WhenOwner_ShouldSucceed() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().commentCount(1).build();
        Comment comment = Comment.builder().id(commentId).user(user).post(post).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(user));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId);

        verify(commentRepository).delete(comment);
        assertThat(post.getCommentCount()).isEqualTo(0);
    }

    @Test
    void createReply_WhenValid_ShouldSucceed() {
        UUID postId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).allowComments(true).commentCount(1).build();
        Comment parentComment = Comment.builder().id(parentId).post(post).build();
        
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Reply");
        request.setParentCommentId(parentId);

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        CommentDTO result = commentService.createComment(postId, request);

        assertThat(result.getParentCommentId()).isEqualTo(parentId);
        assertThat(post.getCommentCount()).isEqualTo(2);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getParentComment().getId()).isEqualTo(parentId);
    }

    @Test
    void createReplyToReply_WhenValid_ShouldReparentToTopLevelParent() {
        UUID postId = UUID.randomUUID();
        UUID topLevelParentId = UUID.randomUUID();
        UUID nestedReplyId = UUID.randomUUID();
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).allowComments(true).commentCount(2).build();
        Comment topLevelParent = Comment.builder().id(topLevelParentId).post(post).build();
        Comment nestedReply = Comment.builder().id(nestedReplyId).post(post).parentComment(topLevelParent).build();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Reply to reply");
        request.setParentCommentId(nestedReplyId);

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(nestedReplyId)).thenReturn(Optional.of(nestedReply));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

        CommentDTO result = commentService.createComment(postId, request);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getParentComment().getId()).isEqualTo(topLevelParentId);
        assertThat(result.getParentCommentId()).isEqualTo(topLevelParentId);
        assertThat(post.getCommentCount()).isEqualTo(3);
    }

    @Test
    void createReply_WhenParentCommentBelongsToDifferentPost_ShouldThrowException() {
        UUID postId = UUID.randomUUID();
        UUID parentPostId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        authenticateUser("me");

        Post post = Post.builder().id(postId).allowComments(true).commentCount(0).build();
        Post parentPost = Post.builder().id(parentPostId).build();
        Comment parentComment = Comment.builder().id(parentId).post(parentPost).build();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Reply");
        request.setParentCommentId(parentId);

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));

        assertThatThrownBy(() -> commentService.createComment(postId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent comment must belong to the same post");
    }
}
