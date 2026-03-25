package com.nchuy099.mini_instagram.story.entity;

import com.nchuy099.mini_instagram.common.entity.BaseEntity;
import com.nchuy099.mini_instagram.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Entity
@Table(name = "stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Story extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "media_url", nullable = false)
    private String mediaUrl;

    @Column(name = "media_type", length = 20)
    private String mediaType; // IMAGE, VIDEO

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;
}
