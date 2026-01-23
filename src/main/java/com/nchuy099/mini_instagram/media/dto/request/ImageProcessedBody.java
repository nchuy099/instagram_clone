package com.nchuy099.mini_instagram.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageProcessedBody {

    @NotBlank
    private String type;

    @NotBlank
    private String ownerId;

    @NotBlank
    private String fileId;

    @NotNull
    private ImageDto image;

    @Getter
    @Setter
    public static class ImageDto {
        @NotBlank
        String url;

        @NotBlank
        String contentType;
    }

}
