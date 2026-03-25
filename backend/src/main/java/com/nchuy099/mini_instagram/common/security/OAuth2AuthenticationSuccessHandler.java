package com.nchuy099.mini_instagram.common.security;

import com.nchuy099.mini_instagram.auth.dto.AuthResponse;
import com.nchuy099.mini_instagram.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            @Lazy AuthService authService,
            @Value("${frontend_url}") String frontendUrl
    ) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        AuthResponse authResponse = authService.createRefreshTokenForUser(oAuth2User.getEmail());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth2/callback")
                .queryParam("token", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("isUsernameSet", oAuth2User.isUsernameSet())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
