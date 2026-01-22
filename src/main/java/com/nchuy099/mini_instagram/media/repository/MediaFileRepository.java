package com.nchuy099.mini_instagram.media.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nchuy099.mini_instagram.media.entity.MediaFileEntity;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFileEntity, UUID> {

}
