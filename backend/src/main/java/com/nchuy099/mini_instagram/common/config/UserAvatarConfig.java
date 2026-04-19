package com.nchuy099.mini_instagram.common.config;

import com.nchuy099.mini_instagram.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserAvatarConfig {

    public UserAvatarConfig(@Value("${app.user.default-avatar-url}") String defaultAvatarUrl) {
        User.setDefaultAvatarUrl(defaultAvatarUrl);
    }
}
