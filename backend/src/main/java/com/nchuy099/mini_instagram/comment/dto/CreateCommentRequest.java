package com.nchuy099.mini_instagram.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {
    @NotBlank(message = "Comment content cannot be empty")
    private String content;
    
    private UUID parentCommentId;
}
