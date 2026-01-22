package com.nchuy099.mini_instagram.post.entity;

import com.nchuy099.mini_instagram.common.AbstractEntity;
import com.nchuy099.mini_instagram.media.entity.MediaContainerEntity;
import com.nchuy099.mini_instagram.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "posts")
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostEntity extends AbstractEntity {

    @Column(nullable = false)
    String caption;

    String locationText;

    String locationLat;

    String locationLng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    UserEntity owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_container_id", nullable = false)
    MediaContainerEntity mediaContainer;

}
