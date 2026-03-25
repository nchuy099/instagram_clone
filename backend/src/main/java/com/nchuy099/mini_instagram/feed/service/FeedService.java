package com.nchuy099.mini_instagram.feed.service;

import com.nchuy099.mini_instagram.post.dto.PostDTO;
import com.nchuy099.mini_instagram.common.response.PagedResponse;

public interface FeedService {
    PagedResponse<PostDTO> getFollowingFeed(int page, int size);
    PagedResponse<PostDTO> getExploreFeed(int page, int size);
}
