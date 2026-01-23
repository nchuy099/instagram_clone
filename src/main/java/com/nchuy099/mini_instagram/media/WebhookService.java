package com.nchuy099.mini_instagram.media;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.enums.MediaStatus;
import com.nchuy099.mini_instagram.common.enums.MediaType;
import com.nchuy099.mini_instagram.common.enums.UploadType;
import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.media.dto.request.ImageProcessedBody;
import com.nchuy099.mini_instagram.media.entity.MediaContainerEntity;
import com.nchuy099.mini_instagram.media.entity.MediaFileEntity;
import com.nchuy099.mini_instagram.media.repository.MediaContainerRepository;
import com.nchuy099.mini_instagram.media.repository.MediaFileRepository;
import com.nchuy099.mini_instagram.user.AvatarEntity;
import com.nchuy099.mini_instagram.user.UserEntity;
import com.nchuy099.mini_instagram.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final MediaFileRepository mediaFileRepository;
    private final MediaContainerRepository mediaContainerRepository;
    private final UserRepository userRepository;

    @Value("${webhook.secret}")
    private String webhookSecret;

    public void verifySignature(String signature, ImageProcessedBody body) {

        log.info("Verifying signature");
        try {
            String bodyString = objectMapper.writeValueAsString(body);

            String expectedSignature = hmacSha256(bodyString, webhookSecret);

            if (!signature.equals(expectedSignature)) {
                log.warn("Invalid signature");
                throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid signature");
            }

            log.info("Verified! Valid signature");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize body: " + e.getMessage());
            throw new RuntimeException("Failed to serialize body: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void processWebhook(ImageProcessedBody body) {
        log.info("Processing Image processed webhook request");

        UploadType type = UploadType.valueOf(body.getType());

        String ownerId = body.getOwnerId();
        String fileId = body.getFileId();
        String url = body.getImage().getUrl();
        String contentType = body.getImage().getContentType();

        if (type == UploadType.POST) {
            MediaContainerEntity mediaContainerEntity = mediaContainerRepository.findById(UUID.fromString(ownerId))
                    .orElseThrow(() -> {
                        log.warn("Media container not found");
                        throw new AppException(ErrorCode.NOT_FOUND, "Media container not found");
                    });

            MediaFileEntity mediaFileEntity = MediaFileEntity.builder()
                    .contentType(contentType)
                    .url(url)
                    .mediaType(MediaType.IMAGE)
                    .status(MediaStatus.READY)
                    .mediaContainer(mediaContainerEntity)
                    .build();

            mediaFileRepository.save(mediaFileEntity);
        } else {
            UserEntity userEntity = userRepository.findById(UUID.fromString(ownerId))
                    .orElseThrow(() -> {
                        log.warn("User not found");
                        throw new AppException(ErrorCode.NOT_FOUND, "User not found");
                    });
            // deactivate ALL active avatars
            userEntity.getAvatars()
                    .stream()
                    .filter(AvatarEntity::isActive)
                    .forEach(a -> a.setActive(false));
            AvatarEntity newAva = AvatarEntity.builder()
                    .url(url)
                    .contentType(contentType)
                    .isActive(true)
                    .user(userEntity)
                    .build();
            userEntity.getAvatars().add(newAva);
            userRepository.save(userEntity);
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }

}
