package com.nchuy099.mini_instagram.post.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.common.security.SecurityConfig;
import com.nchuy099.mini_instagram.common.utils.CursorUtils;
import com.nchuy099.mini_instagram.common.utils.SecurityUtils;
import com.nchuy099.mini_instagram.common.utils.CursorUtils.CursorData;
import com.nchuy099.mini_instagram.media.entity.MediaContainerEntity;
import com.nchuy099.mini_instagram.media.entity.MediaFileEntity;
import com.nchuy099.mini_instagram.media.repository.MediaContainerRepository;
import com.nchuy099.mini_instagram.post.dto.request.PublishPostReq;
import com.nchuy099.mini_instagram.post.dto.response.FeedPageResp;
import com.nchuy099.mini_instagram.post.dto.response.PostFeedResp;
import com.nchuy099.mini_instagram.post.dto.response.PublishPostResp;
import com.nchuy099.mini_instagram.post.dto.response.PostFeedResp.LocationDto;
import com.nchuy099.mini_instagram.post.dto.response.PostFeedResp.MediaDto;
import com.nchuy099.mini_instagram.post.entity.PostEntity;
import com.nchuy099.mini_instagram.post.repository.PostRepository;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MediaContainerRepository mediaContainerRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public PublishPostResp publish(PublishPostReq req, String containerIdString) {
        log.info("Processing Publish post request");

        UUID containerId = UUID.fromString(containerIdString);

        Optional<MediaContainerEntity> mediaConOpt = mediaContainerRepository.findById(containerId);
        if (mediaConOpt.isEmpty()) {
            log.warn("Media container with ID {} not found", containerId);
            throw new AppException(ErrorCode.NOT_FOUND, "Media container not found");
        }

        MediaContainerEntity mediaContainer = mediaConOpt.get();

        PostEntity postEntity = PostEntity.builder()
                .caption(req.getCaption())
                .locationText(req.getLocation().getText())
                .locationLat(req.getLocation().getLat())
                .locationLng(req.getLocation().getLng())
                .mediaContainer(mediaContainer)
                .owner(mediaContainer.getOwner())
                .build();

        postRepository.save(postEntity);

        return PublishPostResp.builder()
                .postId(postEntity.getId().toString())
                .build();
    }

    public FeedPageResp getFeedPage(String cursor) {

        CursorUtils.CursorData cursorData = CursorUtils.decode(cursor);
        Instant createdAt;
        UUID postId;
        if (cursorData == null) {
            createdAt = Instant.now();
            postId = UUID.randomUUID();
        } else {
            createdAt = cursorData.createdAt();
            postId = UUID.fromString(cursorData.id());
        }

        UUID userId = securityUtils.getCurrentUserId();

        int pageSize = 10;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<PostEntity> posts = postRepository.findFeedPosts(postId, userId, createdAt, pageable);

        boolean hasMore = posts.size() > pageSize;
        if (hasMore) {
            posts = posts.subList(0, pageSize);
        }

        String nextCursor = null;

        if (!posts.isEmpty()) {
            PostEntity lastPost = posts.get(posts.size() - 1);

            nextCursor = CursorUtils.encode(lastPost.getCreatedAt(), lastPost.getId().toString());
        }

        return FeedPageResp.builder()
                .posts(posts.stream().map(this::toPostFeedResp).toList())
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private PostFeedResp toPostFeedResp(PostEntity post) {
        return PostFeedResp.builder()

                .postId(post.getId().toString())
                .caption(post.getCaption())
                .publishedAt(post.getCreatedAt())

                .location(LocationDto.builder()
                        .text(post.getLocationText())
                        .lat(post.getLocationLat())
                        .lng(post.getLocationLng())
                        .build())

                .username(post.getOwner().getUsername())
                .userAvatar(post.getOwner().getAvatarUrl())

                .media(post.getMediaContainer().getMediaFiles().stream().map(this::toMediaDto).toList())
                .build();

    }

    private MediaDto toMediaDto(MediaFileEntity media) {
        return MediaDto.builder()
                .url(media.getUrl())
                .contentType(media.getContentType())
                .build();
    }
}
