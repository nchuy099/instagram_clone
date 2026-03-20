package com.nchuy099.mini_instagram.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.nchuy099.mini_instagram.common.entity.BaseEntity;

import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private boolean isPrivate = false;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Integer postCount = 0;

    @Column(name = "follower_count", nullable = false)
    @Builder.Default
    private Integer followerCount = 0;

    @Column(name = "following_count", nullable = false)
    @Builder.Default
    private Integer followingCount = 0;
}
