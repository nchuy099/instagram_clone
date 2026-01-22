package com.nchuy099.mini_instagram.media.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nchuy099.mini_instagram.media.entity.MediaContainerEntity;

@Repository
public interface MediaContainerRepository extends JpaRepository<MediaContainerEntity, UUID> {

}
