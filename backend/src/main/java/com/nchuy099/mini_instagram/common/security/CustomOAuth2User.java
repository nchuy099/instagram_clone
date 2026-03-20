package com.nchuy099.mini_instagram.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final String localUsername;
    private final String email;
    private final boolean isUsernameSet;

    public CustomOAuth2User(OAuth2User oauth2User, String localUsername, String email, boolean isUsernameSet) {
        this.oauth2User = oauth2User;
        this.localUsername = localUsername;
        this.email = email;
        this.isUsernameSet = isUsernameSet;
    }

    public boolean isUsernameSet() {
        return isUsernameSet;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return email != null ? email : localUsername;
    }

    public String getEmail() {
        return email;
    }

    public String getLocalUsername() {
        return localUsername;
    }

    public OAuth2User getOauth2User() {
        return oauth2User;
    }
}
