package com.nchuy099.mini_instagram.media.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMediaContainerReq {

    @NotBlank
    private String mediaContentType;
}
