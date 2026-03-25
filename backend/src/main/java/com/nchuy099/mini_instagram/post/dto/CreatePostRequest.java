package com.nchuy099.mini_instagram.post.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private String caption;
    private String location;

    @NotEmpty(message = "Post must have at least one media item")
    private List<PostMediaRequest> media;

    private boolean allowComments = true;

    @Data
    public static class PostMediaRequest {
        @NotEmpty(message = "Media URL is required")
        private String url;

        @NotEmpty(message = "Media type is required")
        private String type; // IMAGE or VIDEO

        private String thumbnailUrl;

        private Integer orderIndex = 0;
    }
}
