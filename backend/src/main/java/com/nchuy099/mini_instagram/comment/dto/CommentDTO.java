package com.nchuy099.mini_instagram.comment.dto;

import com.nchuy099.mini_instagram.user.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentDTO {
    private UUID id;
    private String content;
    private UserDTO user;
    private UUID postId;
    private UUID parentCommentId;
    private LocalDateTime createdAt;
    private Integer replyCount;
}
