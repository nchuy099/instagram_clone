package com.nchuy099.mini_instagram.search.service;

import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.entity.PostMedia;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.search.dto.*;
import com.nchuy099.mini_instagram.search.entity.RecentSearch;
import com.nchuy099.mini_instagram.search.repository.RecentSearchRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final Pattern HASHTAG_IN_QUERY_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final RecentSearchRepository recentSearchRepository;

    @Transactional
    public SearchResultDTO searchAll(String query) {
        String q = normalizeQuery(query);
        saveRecentSearch(RecentSearch.SearchType.ALL, q);

        if (containsHashtagQuery(q)) {
            String hashtagQuery = normalizeHashtagQuery(q);
            return SearchResultDTO.builder()
                    .users(List.of())
                    .hashtags(searchHashtagsInternal(hashtagQuery))
                    .posts(searchPostsByHashtagInternal(hashtagQuery))
                    .build();
        }

        return SearchResultDTO.builder()
                .users(searchUsersInternal(q))
                .hashtags(searchHashtagsInternal(q))
                .posts(searchPostsInternal(q))
                .build();
    }

    @Transactional
    public List<SearchUserDTO> searchUsers(String query) {
        String q = normalizeQuery(query);
        return searchUsersInternal(q);
    }

    @Transactional
    public List<SearchHashtagDTO> searchHashtags(String query) {
        String q = normalizeHashtagQuery(query);
        List<SearchHashtagDTO> results = searchHashtagsInternal(q);

        saveRecentSearch(RecentSearch.SearchType.HASHTAG, q);
        return results;
    }

    @Transactional
    public List<SearchPostDTO> searchPosts(String query) {
        String q = normalizeQuery(query);
        List<SearchPostDTO> results = containsHashtagQuery(q)
                ? searchPostsByHashtagInternal(normalizeHashtagQuery(q))
                : searchPostsInternal(q);

        saveRecentSearch(RecentSearch.SearchType.POST, q);
        return results;
    }

    @Transactional(readOnly = true)
    public List<RecentSearchDTO> getRecentSearches() {
        User currentUser = getCurrentAuthenticatedUser();
        return recentSearchRepository.findTop7ByUserIdOrderByUpdatedAtDesc(currentUser.getId()).stream()
                .map(search -> RecentSearchDTO.builder()
                        .id(search.getId())
                        .searchType(search.getSearchType())
                        .queryText(search.getQueryText())
                        .createdAt(search.getCreatedAt())
                        .user(search.getTargetUser() == null ? null : RecentSearchUserDTO.builder()
                                .id(search.getTargetUser().getId())
                                .username(search.getTargetUser().getUsername())
                                .fullName(search.getTargetUser().getFullName())
                                .avatarUrl(search.getTargetUser().getAvatarUrl())
                                .build())
                        .build())
                .toList();
    }

    @Transactional
    public void trackUserResultClick(UUID targetUserId, String queryText) {
        User currentUser = getCurrentAuthenticatedUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        String normalizedQuery = (queryText == null || queryText.isBlank())
                ? targetUser.getUsername()
                : queryText.trim();

        RecentSearch recentSearch = recentSearchRepository
                .findByUserIdAndSearchTypeAndTargetUserId(currentUser.getId(), RecentSearch.SearchType.USER, targetUser.getId())
                .orElseGet(() -> RecentSearch.builder()
                        .user(currentUser)
                        .searchType(RecentSearch.SearchType.USER)
                        .targetUser(targetUser)
                        .queryText(normalizedQuery)
                        .build());

        recentSearch.setTargetUser(targetUser);
        recentSearch.setQueryText(normalizedQuery);
        recentSearchRepository.save(recentSearch);
    }

    @Transactional
    public void deleteRecentSearch(UUID id) {
        User currentUser = getCurrentAuthenticatedUser();
        RecentSearch search = recentSearchRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recent search not found"));
        recentSearchRepository.delete(search);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        return query.trim();
    }

    private String normalizeHashtagQuery(String query) {
        String normalized = normalizeQuery(query);
        Matcher matcher = HASHTAG_IN_QUERY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            normalized = matcher.group(1);
        } else if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        normalized = normalized.replace("#", "").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        return normalized;
    }

    private boolean containsHashtagQuery(String query) {
        return query.contains("#");
    }

    private List<SearchUserDTO> searchUsersInternal(String query) {
        return userRepository.searchUsers(query).stream()
                .limit(DEFAULT_LIMIT)
                .map(user -> SearchUserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .toList();
    }

    private List<SearchHashtagDTO> searchHashtagsInternal(String query) {
        return postRepository.searchHashtags(query).stream()
                .limit(DEFAULT_LIMIT)
                .map(tag -> SearchHashtagDTO.builder().name(tag).build())
                .toList();
    }

    private List<SearchPostDTO> searchPostsInternal(String query) {
        return postRepository.searchPostsByCaption(query).stream()
                .limit(DEFAULT_LIMIT)
                .map(this::mapPostToSearchPostDto)
                .toList();
    }

    private List<SearchPostDTO> searchPostsByHashtagInternal(String hashtag) {
        return postRepository.searchPostsByHashtag(hashtag).stream()
                .limit(DEFAULT_LIMIT)
                .map(this::mapPostToSearchPostDto)
                .toList();
    }

    private SearchPostDTO mapPostToSearchPostDto(Post post) {
        return SearchPostDTO.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .thumbnailUrl(resolveThumbnailUrl(post))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .userId(post.getUser().getId())
                .username(post.getUser().getUsername())
                .build();
    }

    private String resolveThumbnailUrl(Post post) {
        return post.getMedia().stream()
                .sorted(Comparator.comparing(PostMedia::getOrderIndex))
                .map(PostMedia::getUrl)
                .findFirst()
                .orElse(null);
    }

    private void saveRecentSearch(RecentSearch.SearchType searchType, String query) {
        User currentUser = getCurrentAuthenticatedUser();
        RecentSearch recentSearch = recentSearchRepository
                .findByUserIdAndSearchTypeAndQueryTextIgnoreCase(currentUser.getId(), searchType, query)
                .orElseGet(() -> RecentSearch.builder()
                        .user(currentUser)
                        .searchType(searchType)
                        .targetUser(null)
                        .queryText(query)
                        .build());

        recentSearch.setTargetUser(null);
        recentSearch.setQueryText(query);
        recentSearchRepository.save(recentSearch);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Authentication required");
        }

        String credential = authentication.getName();
        return userRepository.findByUsernameOrEmailOrPhoneNumber(credential, credential, credential)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}
