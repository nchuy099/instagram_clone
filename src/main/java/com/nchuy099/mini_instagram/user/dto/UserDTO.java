package com.nchuy099.mini_instagram.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String bio;
}
