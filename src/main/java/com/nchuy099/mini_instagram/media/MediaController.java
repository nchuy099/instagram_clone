package com.nchuy099.mini_instagram.media;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.media.dto.request.CreateMediaContainerReq;
import com.nchuy099.mini_instagram.media.dto.response.CreateMediaContainerResp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/media")
@Slf4j
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/containers/create")
    public CreateMediaContainerResp createMediaContainer(@RequestBody CreateMediaContainerReq req) {
        log.info("Create Media Container request received");

        return mediaService.create(req);
    }

}
