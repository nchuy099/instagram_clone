package com.nchuy099.mini_instagram.notification.controller;

import com.nchuy099.mini_instagram.common.response.PagedResponse;
import com.nchuy099.mini_instagram.common.security.CustomUserDetailsService;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationEntryPoint;
import com.nchuy099.mini_instagram.common.security.JwtAuthenticationFilter;
import com.nchuy099.mini_instagram.common.security.JwtTokenProvider;
import com.nchuy099.mini_instagram.notification.dto.NotificationDTO;
import com.nchuy099.mini_instagram.notification.entity.NotificationType;
import com.nchuy099.mini_instagram.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldGetNotifications() throws Exception {
        NotificationDTO item = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.POST_LIKE)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .actorId(UUID.randomUUID())
                .actorUsername("actor")
                .actorAvatarUrl("https://example.com/actor.jpg")
                .postId(UUID.randomUUID())
                .build();

        PagedResponse<NotificationDTO> page = PagedResponse.<NotificationDTO>builder()
                .content(List.of(item))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(notificationService.getNotifications(0, 20, true)).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(item.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].type").value("POST_LIKE"));
    }

    @Test
    void shouldMarkNotificationRead() throws Exception {
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void shouldReturnUnauthorizedWhenMarkReadForOtherRecipient() throws Exception {
        UUID notificationId = UUID.randomUUID();
        doThrow(new IllegalStateException("Not authorized to update this notification"))
                .when(notificationService)
                .markAsRead(notificationId);

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Not authorized to update this notification"));
    }

    @Test
    void shouldMarkAllNotificationsRead() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(5);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead();
    }
}
