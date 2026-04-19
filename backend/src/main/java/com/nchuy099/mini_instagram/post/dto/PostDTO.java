package com.nchuy099.mini_instagram.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nchuy099.mini_instagram.user.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PostDTO {
    private UUID id;
    private String caption;
    private String location;
    private UserDTO user;
    private List<PostMediaDTO> media;
    private Integer likeCount;
    private Integer commentCount;
    private boolean allowComments;
    private LocalDateTime createdAt;
    
    // Viewer state
    @JsonProperty("isLiked")
    private boolean liked;
    @JsonProperty("isSaved")
    private boolean saved;
    @JsonProperty("isFollowing")
    private boolean following;
}
