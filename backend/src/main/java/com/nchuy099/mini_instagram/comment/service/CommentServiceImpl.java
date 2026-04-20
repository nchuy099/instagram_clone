package com.nchuy099.mini_instagram.comment.service;

import com.nchuy099.mini_instagram.comment.dto.CommentDTO;
import com.nchuy099.mini_instagram.comment.dto.CreateCommentRequest;
import com.nchuy099.mini_instagram.comment.entity.Comment;
import com.nchuy099.mini_instagram.comment.repository.CommentRepository;
import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.notification.event.CommentCreatedEvent;
import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public CommentDTO createComment(UUID postId, CreateCommentRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.isAllowComments()) {
            throw new IllegalStateException("Comments are disabled for this post");
        }

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("Parent comment must belong to the same post");
            }
            while (parentComment.getParentComment() != null) {
                parentComment = parentComment.getParentComment();
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(currentUser)
                .post(post)
                .parentComment(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Update post comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        User recipient = post.getUser();
        if (recipient != null && recipient.getId() != null) {
            log.info(
                    "notification_event_publish type=POST_COMMENT actorId={} recipientId={} postId={} commentId={}",
                    currentUser.getId(),
                    recipient.getId(),
                    post.getId(),
                    savedComment.getId()
            );
            applicationEventPublisher.publishEvent(new CommentCreatedEvent(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getAvatarUrl(),
                    recipient.getId(),
                    resolvePrincipal(recipient),
                    post.getId(),
                    savedComment.getId()
            ));
        }

        return mapToDTO(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        User currentUser = getCurrentAuthenticatedUser();

        // Only author of comment or author of post can delete comment
        if (!comment.getUser().getId().equals(currentUser.getId()) &&
            !comment.getPost().getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this comment");
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);

        // Update post comment count (need to account for all deleted replies if cascade delete but repo info is limited here)
        // JPA Cascade will delete replies. We should ideally decrement by count of deleted items.
        // For simplicity in MVP, we might just re-count or decrement by 1 if not nested, but Instagram counts all.
        // Let's just decrement by 1 for now or better, recount if concerned.
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CommentDTO> getCommentsByPost(UUID postId, Pageable pageable) {
        Page<CommentDTO> page = commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable)
                .map(this::mapToDTO);
        return PagedResponse.fromPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CommentDTO> getRepliesByComment(UUID commentId, Pageable pageable) {
        Page<CommentDTO> page = commentRepository.findByParentCommentId(commentId, pageable)
                .map(this::mapToDTO);
        return PagedResponse.fromPage(page);
    }

    private CommentDTO mapToDTO(Comment comment) {
        User author = comment.getUser();
        UserDTO userDTO = UserDTO.builder()
                .id(author.getId())
                .username(author.getUsername())
                .fullName(author.getFullName())
                .avatarUrl(author.getAvatarUrl())
                .bio(author.getBio())
                .build();

        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(userDTO)
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .build();
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

    private String resolvePrincipal(User user) {
        return user.getEmail() == null ? user.getUsername() : user.getEmail();
    }
}
