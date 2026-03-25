package com.nchuy099.mini_instagram.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResultDTO {
    private List<SearchUserDTO> users;
    private List<SearchHashtagDTO> hashtags;
    private List<SearchPostDTO> posts;
}
