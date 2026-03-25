package com.nchuy099.mini_instagram.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SearchUserDTO {
    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
}
