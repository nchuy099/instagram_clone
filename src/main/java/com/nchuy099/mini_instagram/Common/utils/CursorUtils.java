package com.nchuy099.mini_instagram.common.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class CursorUtils {
    public static final String SEPARATOR = "||";

    private CursorUtils() {
    } // chặn new

    // 1. Mã hóa: Gom Date + ID -> String -> Base64
    public static String encode(Instant createdAt, String id) {
        if (createdAt == null || id == null) {
            return null;
        }
        // Kết quả raw: "2023-10-27T10:00:00Z||post_123"
        String raw = createdAt.toString() + SEPARATOR + id;

        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // 2. Giải mã: Base64 -> String -> Tách ra Date & ID
    public static CursorData decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.trim().isEmpty()) {
            return null;
        }

        try {
            // Decode
            byte[] decodedBytes = Base64.getDecoder().decode(encodedCursor);
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);

            // Tách chuỗi dựa trên SEPARATOR
            // Lưu ý: split cần regex nên cần escape dấu |
            String[] parts = raw.split("\\|\\|");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }

            return new CursorData(Instant.parse(parts[0]), parts[1]);
        } catch (Exception e) {
            // Nếu client gửi cursor bậy bạ, ta ném lỗi hoặc return null để load trang 1
            throw new IllegalArgumentException("Malformed cursor provided", e);
        }
    }

    public record CursorData(Instant createdAt, String id) {
    }
}
