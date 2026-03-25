package com.nchuy099.mini_instagram.search.entity;

import com.nchuy099.mini_instagram.common.entity.BaseEntity;
import com.nchuy099.mini_instagram.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recent_searches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecentSearch extends BaseEntity {

    public enum SearchType {
        ALL,
        USER,
        HASHTAG,
        POST
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 20)
    private SearchType searchType;

    @Column(name = "query_text", nullable = false, length = 255)
    private String queryText;
}
