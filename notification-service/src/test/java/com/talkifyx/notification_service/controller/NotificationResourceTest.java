package com.talkifyx.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkifyx.notification_service.dto.NotificationResponse;
import com.talkifyx.notification_service.dto.SendNotificationRequest;
import com.talkifyx.notification_service.entity.NotificationType;
import com.talkifyx.notification_service.exception.ResourceNotFoundException;
import com.talkifyx.notification_service.resource.NotificationResource;
import com.talkifyx.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationResource.class)
class NotificationResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private NotificationResponse response;
    private SendNotificationRequest request;

    @BeforeEach
    void setUp() {
        response = new NotificationResponse();
        response.setNotificationId("notif-1");
        response.setRecipientId(1L);
        response.setActorId(2L);
        response.setType(NotificationType.NEW_MESSAGE);
        response.setTitle("Hello");
        response.setMessage("You have a message");
        response.setRoomId(10L);
        response.setMessageId("msg-1");
        response.setRead(false);
        response.setCreatedAt(LocalDateTime.now());

        request = new SendNotificationRequest();
        request.setRecipientId(1L);
        request.setType(NotificationType.NEW_MESSAGE);
        request.setTitle("Hello");
        request.setMessage("You have a message");
    }

    @Test
    void send_ValidRequest_Returns200() throws Exception {
        when(notificationService.send(any(SendNotificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello"))
                .andExpect(jsonPath("$.recipientId").value(1));
    }

    @Test
    void send_MissingRecipientId_Returns400() throws Exception {
        SendNotificationRequest bad = new SendNotificationRequest();
        bad.setType(NotificationType.NEW_MESSAGE);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void send_MissingType_Returns400() throws Exception {
        SendNotificationRequest bad = new SendNotificationRequest();
        bad.setRecipientId(1L);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_Returns200WithPage() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response));
        when(notificationService.getUserNotifications(1L, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Hello"));
    }

    @Test
    void getUserNotifications_WithCustomPageParams_Returns200() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response));
        when(notificationService.getUserNotifications(1L, 1, 5)).thenReturn(page);

        mockMvc.perform(get("/api/notifications/user/1?page=1&size=5"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadCount_Returns200WithCount() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/user/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void markAsRead_ValidId_Returns200() throws Exception {
        response.setRead(true);
        when(notificationService.markAsRead("notif-1")).thenReturn(response);

        mockMvc.perform(put("/api/notifications/notif-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markAsRead_NotFound_Returns404() throws Exception {
        when(notificationService.markAsRead("bad-id"))
                .thenThrow(new ResourceNotFoundException("Notification not found: bad-id"));

        mockMvc.perform(put("/api/notifications/bad-id/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_Returns204() throws Exception {
        doNothing().when(notificationService).markAllAsRead(1L);

        mockMvc.perform(put("/api/notifications/user/1/read-all"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_ValidId_Returns204() throws Exception {
        doNothing().when(notificationService).delete("notif-1");

        mockMvc.perform(delete("/api/notifications/notif-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Notification not found: bad-id"))
                .when(notificationService).delete("bad-id");

        mockMvc.perform(delete("/api/notifications/bad-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByType_Returns200WithPage() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of(response));
        when(notificationService.filterByType(1L, NotificationType.NEW_MESSAGE, 0, 20))
                .thenReturn(page);

        mockMvc.perform(get("/api/notifications/user/1/filter?type=NEW_MESSAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("NEW_MESSAGE"));
    }
}