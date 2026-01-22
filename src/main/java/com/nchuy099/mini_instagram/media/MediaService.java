package com.nchuy099.mini_instagram.media;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import javax.print.attribute.standard.Media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import com.nchuy099.mini_instagram.common.enums.UploadType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.common.utils.SecurityUtils;
import com.nchuy099.mini_instagram.media.dto.request.CreateMediaContainerReq;
import com.nchuy099.mini_instagram.media.dto.response.CreateMediaContainerResp;
import com.nchuy099.mini_instagram.media.entity.MediaContainerEntity;
import com.nchuy099.mini_instagram.media.repository.MediaContainerRepository;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaService {

    private final SecurityUtils securityUtils;
    private final MediaContainerRepository mediaContainerRepository;
    private final UserRepository userRepository;
    private final S3Presigner s3Presigner;

    @Value("${media-container-expiration-minutes}")
    private int mediaContainerExpirationMins;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key-id}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key}")
    private String secretAccessKey;

    public CreateMediaContainerResp create(CreateMediaContainerReq req) {
        log.info("Creating media container with request: {}", req);

        UUID userId = securityUtils.getCurrentUserId();

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User with ID {} not found", userId);
            throw new AppException(ErrorCode.NOT_FOUND, "User not found");
        }

        MediaContainerEntity mediaContainer = MediaContainerEntity.builder()
                .owner(userOpt.get())
                .expiresAt(Instant.now().plus(mediaContainerExpirationMins, ChronoUnit.MINUTES))
                .build();

        mediaContainerRepository.save(mediaContainer);

        String mediaFileId = UUID.randomUUID().toString();
        String mediaContainerId = mediaContainer.getId().toString();

        String uploadUrl = createPreSignedUploadUrl(userId.toString(), mediaContainerId,
                mediaFileId, req.getMediaContentType(), UploadType.POST);

        return CreateMediaContainerResp.builder()
                .containerId(mediaContainerId)
                .uploadUrl(uploadUrl)
                .build();
    }

    private String createPreSignedUploadUrl(
            String userId, String containerId,
            String mediaFileId, String contentType,
            UploadType uploadType) {

        log.info("Creating pre-signed upload URL");

        String key = generateS3ObjectKey(userId, containerId, mediaFileId, contentType, uploadType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.of(mediaContainerExpirationMins, ChronoUnit.MINUTES))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    private String generateS3ObjectKey(
            String userId, String containerId,
            String mediaFileId, String contentType,
            UploadType uploadType) {

        log.info("Generate S3 Object Key");
        String extension = contentType.split("/")[1];

        String key;

        if (uploadType == UploadType.AVATAR) {
            UUID avatarId = UUID.randomUUID();
            key = String.format(
                    "images/avatars/original/%s/%s.%s",
                    userId,
                    avatarId,
                    extension);
        } else {
            key = String.format(
                    "images/posts/original/%s/%s.%s",
                    containerId,
                    mediaFileId,
                    extension);
        }

        return key;
    }
}
