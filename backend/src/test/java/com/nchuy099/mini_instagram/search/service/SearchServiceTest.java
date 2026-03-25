package com.nchuy099.mini_instagram.search.service;

import com.nchuy099.mini_instagram.post.entity.Post;
import com.nchuy099.mini_instagram.post.entity.PostMedia;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.search.dto.RecentSearchDTO;
import com.nchuy099.mini_instagram.search.dto.SearchHashtagDTO;
import com.nchuy099.mini_instagram.search.dto.SearchResultDTO;
import com.nchuy099.mini_instagram.search.dto.SearchPostDTO;
import com.nchuy099.mini_instagram.search.dto.SearchUserDTO;
import com.nchuy099.mini_instagram.search.entity.RecentSearch;
import com.nchuy099.mini_instagram.search.repository.RecentSearchRepository;
import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private RecentSearchRepository recentSearchRepository;

    @InjectMocks
    private SearchService searchService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void searchUsers_WhenQueryProvided_ShouldReturnMatchedUsersAndSaveRecent() {
        authenticateUser("me");
        User matchedUser = User.builder().id(UUID.randomUUID()).username("alice").fullName("Alice Doe").avatarUrl("avatar").build();

        when(userRepository.searchUsers("ali")).thenReturn(List.of(matchedUser));

        List<SearchUserDTO> result = searchService.searchUsers("ali");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
        verify(recentSearchRepository, never()).save(any(RecentSearch.class));
    }

    @Test
    void searchHashtags_WhenQueryProvided_ShouldReturnMatchedHashtagsAndSaveRecent() {
        authenticateUser("me");
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.searchHashtags("spr")).thenReturn(List.of("springboot"));

        List<SearchHashtagDTO> result = searchService.searchHashtags("spr");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("springboot");
        verify(recentSearchRepository).save(any(RecentSearch.class));
    }

    @Test
    void searchHashtags_WhenQueryContainsHashInMiddle_ShouldExtractHashtagTerm() {
        authenticateUser("me");
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.searchHashtags("java")).thenReturn(List.of("java"));

        List<SearchHashtagDTO> result = searchService.searchHashtags("find #java now");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("java");
        verify(postRepository).searchHashtags("java");
    }

    @Test
    void searchPosts_WhenQueryProvided_ShouldReturnMatchedPostsAndSaveRecent() {
        authenticateUser("me");
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        User author = User.builder().id(UUID.randomUUID()).username("author").fullName("Author").build();

        Post post = Post.builder()
                .id(UUID.randomUUID())
                .user(author)
                .caption("This is a spring post")
                .likeCount(5)
                .commentCount(2)
                .media(List.of(PostMedia.builder().id(UUID.randomUUID()).url("https://img").type(PostMedia.MediaType.IMAGE).orderIndex(0).build()))
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.searchPostsByCaption("spring")).thenReturn(List.of(post));

        List<SearchPostDTO> result = searchService.searchPosts("spring");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCaption()).isEqualTo("This is a spring post");
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("https://img");
        verify(recentSearchRepository).save(any(RecentSearch.class));
    }

    @Test
    void searchPosts_WhenQueryContainsHashtag_ShouldSearchByHashtag() {
        authenticateUser("me");
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        User author = User.builder().id(UUID.randomUUID()).username("author").fullName("Author").build();

        Post post = Post.builder()
                .id(UUID.randomUUID())
                .user(author)
                .caption("#java tips")
                .likeCount(1)
                .commentCount(1)
                .media(List.of(PostMedia.builder().id(UUID.randomUUID()).url("https://img").type(PostMedia.MediaType.IMAGE).orderIndex(0).build()))
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.searchPostsByHashtag("java")).thenReturn(List.of(post));

        List<SearchPostDTO> result = searchService.searchPosts("abc #java xyz");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCaption()).isEqualTo("#java tips");
        verify(postRepository).searchPostsByHashtag("java");
        verify(postRepository, never()).searchPostsByCaption(any());
    }

    @Test
    void searchAll_WhenQueryContainsHashtag_ShouldReturnHashtagAndTaggedPostsOnly() {
        authenticateUser("me");
        User currentUser = User.builder().id(UUID.randomUUID()).username("me").build();
        User author = User.builder().id(UUID.randomUUID()).username("author").fullName("Author").build();

        Post post = Post.builder()
                .id(UUID.randomUUID())
                .user(author)
                .caption("#springboot post")
                .likeCount(2)
                .commentCount(0)
                .media(List.of(PostMedia.builder().id(UUID.randomUUID()).url("https://img").type(PostMedia.MediaType.IMAGE).orderIndex(0).build()))
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(postRepository.searchHashtags("springboot")).thenReturn(List.of("springboot"));
        when(postRepository.searchPostsByHashtag("springboot")).thenReturn(List.of(post));

        SearchResultDTO result = searchService.searchAll("hello #springboot world");

        assertThat(result.getUsers()).isEmpty();
        assertThat(result.getHashtags()).hasSize(1);
        assertThat(result.getPosts()).hasSize(1);
        verify(userRepository, never()).searchUsers(any());
        verify(postRepository, never()).searchPostsByCaption(any());
    }

    @Test
    void getRecentSearches_WhenAuthenticated_ShouldReturnCurrentUserRecentSearches() {
        authenticateUser("me");
        UUID userId = UUID.randomUUID();
        User currentUser = User.builder().id(userId).username("me").build();
        User targetUser = User.builder().id(UUID.randomUUID()).username("alice").fullName("Alice Doe").avatarUrl("avatar").build();

        RecentSearch rs = RecentSearch.builder()
                .id(UUID.randomUUID())
                .queryText("alice")
                .searchType(RecentSearch.SearchType.USER)
                .user(currentUser)
                .targetUser(targetUser)
                .build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(recentSearchRepository.findTop7ByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(rs));

        List<RecentSearchDTO> result = searchService.getRecentSearches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQueryText()).isEqualTo("alice");
        assertThat(result.get(0).getUser()).isNotNull();
        assertThat(result.get(0).getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void trackUserResultClick_WhenValid_ShouldSaveRecentWithTargetUser() {
        authenticateUser("me");
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User currentUser = User.builder().id(currentUserId).username("me").build();
        User targetUser = User.builder().id(targetUserId).username("alice").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(recentSearchRepository.findByUserIdAndSearchTypeAndTargetUserId(currentUserId, RecentSearch.SearchType.USER, targetUserId))
                .thenReturn(Optional.empty());

        searchService.trackUserResultClick(targetUserId, "alice");

        verify(recentSearchRepository).save(any(RecentSearch.class));
    }

    @Test
    void deleteRecentSearch_WhenSearchBelongsToCurrentUser_ShouldDelete() {
        authenticateUser("me");
        UUID userId = UUID.randomUUID();
        UUID searchId = UUID.randomUUID();
        User currentUser = User.builder().id(userId).username("me").build();
        RecentSearch rs = RecentSearch.builder().id(searchId).user(currentUser).queryText("a").searchType(RecentSearch.SearchType.USER).build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(recentSearchRepository.findByIdAndUserId(searchId, userId)).thenReturn(Optional.of(rs));

        searchService.deleteRecentSearch(searchId);

        verify(recentSearchRepository).delete(rs);
    }

    @Test
    void deleteRecentSearch_WhenSearchNotBelongToCurrentUser_ShouldThrow() {
        authenticateUser("me");
        UUID userId = UUID.randomUUID();
        UUID searchId = UUID.randomUUID();
        User currentUser = User.builder().id(userId).username("me").build();

        when(userRepository.findByUsernameOrEmailOrPhoneNumber("me", "me", "me")).thenReturn(Optional.of(currentUser));
        when(recentSearchRepository.findByIdAndUserId(eq(searchId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.deleteRecentSearch(searchId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recent search not found");
    }
}
