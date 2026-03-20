package com.nchuy099.mini_instagram.common.security;

import com.nchuy099.mini_instagram.user.entity.User;
import com.nchuy099.mini_instagram.user.entity.UserAuthProvider;
import com.nchuy099.mini_instagram.user.repository.UserAuthProviderRepository;
import com.nchuy099.mini_instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerName = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String providerId = oAuth2User.getName(); // For Facebook, this is the id
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        
        String pictureUrl = null;
        if (attributes.containsKey("picture")) {
            Object pictureObj = attributes.get("picture");
            if (pictureObj instanceof Map) {
                Map<?, ?> pictureNode = (Map<?, ?>) pictureObj;
                if (pictureNode.containsKey("data")) {
                    Map<?, ?> dataNode = (Map<?, ?>) pictureNode.get("data");
                    if (dataNode.containsKey("url")) {
                        pictureUrl = (String) dataNode.get("url");
                    }
                }
            } else if (pictureObj instanceof String) {
                pictureUrl = (String) pictureObj;
            }
        }

        User user;
        UserAuthProvider provider = userAuthProviderRepository.findByProviderAndProviderUserId(providerName, providerId)
                .orElse(null);

        if (provider != null) {
            user = provider.getUser();
        } else {
            user = null;
            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            if (user == null) {
                // Use providerId as temporary username
                String username = providerName.toLowerCase() + "_" + providerId;
                
                user = User.builder()
                        .username(username)
                        .fullName(name)
                        .email(email)
                        .avatarUrl(pictureUrl)
                        .isUsernameSet(false) // Mark as pending
                        .build();
                user = userRepository.save(user);
            }

            provider = UserAuthProvider.builder()
                    .user(user)
                    .provider(providerName)
                    .providerUserId(providerId)
                    .build();
            userAuthProviderRepository.save(provider);
        }

        return new CustomOAuth2User(oAuth2User, user.getUsername(), user.getEmail(), user.isUsernameSet());
    }
}
