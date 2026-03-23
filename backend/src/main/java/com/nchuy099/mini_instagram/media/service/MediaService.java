package com.nchuy099.mini_instagram.media.service;

import java.util.UUID;

public interface MediaService {
    String generatePresignedUrl(String fileName, String contentType);
    void deleteFile(String fileUrl);
}
