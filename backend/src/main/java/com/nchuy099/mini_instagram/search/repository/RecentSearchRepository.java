package com.nchuy099.mini_instagram.search.repository;

import com.nchuy099.mini_instagram.search.entity.RecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, UUID> {
    List<RecentSearch> findTop7ByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<RecentSearch> findByIdAndUserId(UUID id, UUID userId);

    Optional<RecentSearch> findByUserIdAndSearchTypeAndTargetUserId(UUID userId, RecentSearch.SearchType searchType, UUID targetUserId);

    Optional<RecentSearch> findByUserIdAndSearchTypeAndQueryTextIgnoreCase(UUID userId, RecentSearch.SearchType searchType, String queryText);
}
