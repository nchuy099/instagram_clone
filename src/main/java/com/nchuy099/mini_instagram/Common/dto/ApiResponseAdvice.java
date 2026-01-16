package com.nchuy099.mini_instagram.common.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.mini_instagram.common.exception.ErrorResponse;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        String packageName = returnType.getContainingClass().getPackage().getName();

        // CHỈ áp dụng cho code nằm trong package root của bạn
        // Ví dụ package của bạn là: com.nchuy099.mini_instagram
        return packageName.startsWith("com.nchuy099.mini_instagram");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        String message = "Success";

        ResponseMessage responseMessage = returnType.getMethodAnnotation(ResponseMessage.class);

        if (responseMessage != null) {
            message = responseMessage.value();
        }

        // Nếu body là null/void thì trả về ApiResponse với data là null
        if (body == null) {
            return new ApiResponse<>(200, message, null);
        }

        // Nếu body đã là ApiResponse hoặc ErrorResponse thì không cần bọc lại
        if (body instanceof ApiResponse) {
            return body;
        }

        if (body instanceof ErrorResponse) {
            return body;
        }

        // Nếu body là String thì cần chuyển đổi sang JSON thủ công tránh
        // StringHTTPMessageConverter
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(new ApiResponse<>(200, message, body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return new ApiResponse<>(200, message, body);
    }
}