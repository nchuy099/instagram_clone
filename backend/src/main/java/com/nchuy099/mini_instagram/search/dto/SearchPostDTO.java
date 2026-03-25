package com.nchuy099.mini_instagram.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SearchPostDTO {
    private UUID id;
    private String caption;
    private String thumbnailUrl;
    private Integer likeCount;
    private Integer commentCount;
    private UUID userId;
    private String username;
}
