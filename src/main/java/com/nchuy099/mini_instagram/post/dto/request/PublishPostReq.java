package com.nchuy099.mini_instagram.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishPostReq {

    @NotBlank
    private String caption;

    private LocationDto location;

    @Getter
    @Setter
    public static class LocationDto {
        String text;
        Float lat;
        Float lng;
    }
}
