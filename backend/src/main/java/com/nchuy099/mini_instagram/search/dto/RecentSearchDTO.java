package com.nchuy099.mini_instagram.search.dto;

import com.nchuy099.mini_instagram.search.entity.RecentSearch;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecentSearchDTO {
    private UUID id;
    private RecentSearch.SearchType searchType;
    private String queryText;
    private LocalDateTime createdAt;
    private RecentSearchUserDTO user;
}
