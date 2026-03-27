package com.nchuy099.mini_instagram.websocket.config;

import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerHeader = accessor.getFirstNativeHeader("Authorization");
            String token = resolveToken(bearerHeader);
            if (token == null || jwtTokenProvider.validateToken(token) == false) {
                throw new IllegalArgumentException("Invalid websocket token");
            }

            String identifier = jwtTokenProvider.getEmailFromJWT(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        }

        return message;
    }

    private String resolveToken(String bearerHeader) {
        if (bearerHeader == null) {
            return null;
        }
        if (bearerHeader.startsWith("Bearer ")) {
            return bearerHeader.substring(7);
        }
        return bearerHeader;
    }
}
