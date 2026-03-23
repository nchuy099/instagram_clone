package com.nchuy099.mini_instagram.post.dto;

import lombok.Data;

@Data
public class UpdatePostRequest {
    private String caption;
    private String location;
    private boolean allowComments;
}
