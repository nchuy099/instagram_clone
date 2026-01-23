package com.nchuy099.mini_instagram.media.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nchuy099.mini_instagram.common.AbstractEntity;
import com.nchuy099.mini_instagram.post.PostEntity;
import com.nchuy099.mini_instagram.user.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "media_containers")
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaContainerEntity extends AbstractEntity {

    Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    UserEntity owner;

    @OneToMany(mappedBy = "mediaContainer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    List<MediaFileEntity> mediaFiles = new ArrayList<>();

    @OneToOne(mappedBy = "mediaContainer", fetch = FetchType.LAZY)
    PostEntity post;
}
