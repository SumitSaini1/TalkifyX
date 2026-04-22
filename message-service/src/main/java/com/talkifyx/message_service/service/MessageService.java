package com.talkifyx.message_service.service;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.List;


public interface MessageService {
    MessageResponse sendMessage(Long senderId, MessageRequest request);
    MessageResponse getMessageById(String messageId);
    PagedResponse<MessageResponse> getMessagesByRoom(Long roomId, int page, int size);
    List<MessageResponse> getMessagesBefore(Long roomId, LocalDateTime before);
    MessageResponse editMessage(String messageId, Long senderId, String newContent);
    void deleteMessage(String messageId, Long senderId);
    List<MessageResponse> searchMessages(Long roomId, String keyword);
    void updateDeliveryStatus(String messageId, DeliveryStatus status);
    long getMessageCount(Long roomId);
    long getUnreadMessages(Long roomId, LocalDateTime after);
    List<MessageResponse> getMessagesSentAtAfter(Long roomId, LocalDateTime after);
}