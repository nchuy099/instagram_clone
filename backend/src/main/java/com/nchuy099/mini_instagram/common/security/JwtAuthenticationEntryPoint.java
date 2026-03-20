package com.nchuy099.mini_instagram.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(false)
                .message("Error")
                .error(ApiResponse.ErrorDetails.builder()
                        .code("UNAUTHORIZED")
                        .message(authException.getMessage())
                        .build())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
