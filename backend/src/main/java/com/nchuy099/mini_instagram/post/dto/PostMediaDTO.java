package com.nchuy099.mini_instagram.post.dto;

import com.nchuy099.mini_instagram.post.entity.PostMedia.MediaType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PostMediaDTO {
    private UUID id;
    private String url;
    private String thumbnailUrl;
    private MediaType type;
    private Integer orderIndex;
}
