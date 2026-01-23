package com.nchuy099.mini_instagram.media;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.common.exception.AppException;
import com.nchuy099.mini_instagram.common.exception.ErrorCode;
import com.nchuy099.mini_instagram.media.dto.request.ImageProcessedBody;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/webhook")
@RestController
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/image-processed")
    public String handleImageProcessed(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestBody @Valid ImageProcessedBody body) {
        log.info(" WEBHOOK RECEIVED, Verifying...");
        if (signature == null || signature.isBlank()) {
            log.warn("Missing signature");
            throw new AppException(ErrorCode.BAD_REQUEST, "Missing signature");
        }

        webhookService.verifySignature(signature, body);

        webhookService.processWebhook(body);
        return "Image Webhook verified and processed";
    }
}
