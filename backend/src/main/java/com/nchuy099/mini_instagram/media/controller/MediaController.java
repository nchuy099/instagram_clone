package com.nchuy099.mini_instagram.media.controller;

import com.nchuy099.mini_instagram.common.response.ApiResponse;
import com.nchuy099.mini_instagram.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<String>> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        String url = mediaService.generatePresignedUrl(fileName, contentType);
        return ResponseEntity.ok(ApiResponse.success(url));
    }
}
