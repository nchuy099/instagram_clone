package com.nchuy099.mini_instagram.post;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nchuy099.mini_instagram.post.dto.request.PublishPostReq;
import com.nchuy099.mini_instagram.post.dto.response.FeedPageResp;
import com.nchuy099.mini_instagram.post.dto.response.PublishPostResp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/posts")
@Slf4j
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/publish/{containerId}")
    public PublishPostResp publishPost(@RequestBody PublishPostReq req, @PathVariable String containerId) {
        log.info("Publish post request received: {}", req);
        return postService.publish(req, containerId);
    }

    @GetMapping("/feed")
    public FeedPageResp getFeedPage(@RequestParam(name = "cursor", required = false) String cursor) {
        log.info("Get feed page request received: {}", cursor);
        return postService.getFeedPage(cursor);
    }

    @GetMapping("/me/list")
    public FeedPageResp getUserPosts(@RequestParam(name = "cursor", required = false) String cursor) {
        log.info("Get user posts request received: {}", cursor);
        return postService.getUserPosts(cursor);
    }
}
