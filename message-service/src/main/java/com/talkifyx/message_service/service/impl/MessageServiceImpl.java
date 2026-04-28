package com.talkifyx.message_service.service.impl;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.*;
import com.talkifyx.message_service.feign.AuthServiceClient;
import com.talkifyx.message_service.feign.RoomServiceClient;
import com.talkifyx.message_service.repository.MessageRepository;
import com.talkifyx.message_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final RoomServiceClient roomServiceClient;
    private final AuthServiceClient authServiceClient;

    private MessageResponse toResponse(Message m) {
        String senderName = null;
        String senderAvatar = null;
        try {
            ApiResponse<SenderDto> res = authServiceClient.getUserById(m.getSenderId());
            if (res != null && res.getData() != null) {
                senderName = res.getData().getFullName();
                senderAvatar = res.getData().getAvatarUrl();
            }
        } catch (Exception ignored) {
        }

        return MessageResponse.builder()
                .messageId(m.getMessageId())
                .roomId(m.getRoomId())
                .senderId(m.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .content(m.getContent())
                .type(m.getType())
                .mediaUrl(m.getMediaUrl())
                .replyToMessageId(m.getReplyToMessageId())
                .isEdited(m.isEdited())
                .isDeleted(m.isDeleted())
                .deliveryStatus(m.getDeliveryStatus())
                .sentAt(m.getSentAt())
                .editedAt(m.getEditedAt())
                .build();
    }

    // @Override
    // public MessageResponse sendMessage(Long senderId, MessageRequest req) {
    // Message message = Message.builder()
    // .roomId(req.getRoomId())
    // .senderId(senderId)
    // .content(req.getContent())
    // .type(req.getType())
    // .mediaUrl(req.getMediaUrl())
    // .replyToMessageId(req.getReplyToMessageId())
    // .build();
    // return toResponse(messageRepository.save(message));
    // }

    @Override
    public MessageResponse sendMessage(Long senderId, MessageRequest req) {
        Message message = Message.builder()
                .roomId(req.getRoomId())
                .senderId(senderId)
                .content(req.getContent())
                .type(req.getType())
                .mediaUrl(req.getMediaUrl())
                .replyToMessageId(req.getReplyToMessageId())
                .build();
        MessageResponse response = toResponse(messageRepository.save(message));
        // Override senderName/senderAvatar from request (skip Feign call)
        if (req.getSenderName() != null)
            response.setSenderName(req.getSenderName());
        if (req.getSenderAvatar() != null)
            response.setSenderAvatar(req.getSenderAvatar());
        return response;
    }

    @Override
    public MessageResponse getMessageById(String messageId) {
        return toResponse(messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found")));
    }

    @Override
    public PagedResponse<MessageResponse> getMessagesByRoom(Long roomId, int page, int size) {
        Page<Message> result = messageRepository
                .findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(roomId, PageRequest.of(page, size));
        return PagedResponse.<MessageResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public List<MessageResponse> getMessagesBefore(Long roomId, LocalDateTime before) {
        return messageRepository.findByRoomIdAndSentAtBefore(roomId, before)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public MessageResponse editMessage(String messageId, Long senderId, String newContent) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!m.getSenderId().equals(senderId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your message");
        if (m.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit deleted message");
        m.setContent(newContent);
        m.setEdited(true);
        m.setEditedAt(LocalDateTime.now());
        return toResponse(messageRepository.save(m));
    }

    @Override
    public void deleteMessage(String messageId, Long senderId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!m.getSenderId().equals(senderId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your message");

        messageRepository.delete(m);
    }

    @Override
    public List<MessageResponse> searchMessages(Long roomId, String keyword) {
        return messageRepository.searchInRoom(roomId, keyword)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void updateDeliveryStatus(String messageId, DeliveryStatus status) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        m.setDeliveryStatus(status);
        messageRepository.save(m);
    }

    @Override
    public long getMessageCount(Long roomId) {
        return messageRepository.countByRoomIdAndIsDeletedFalse(roomId);
    }

    @Override
    public long getUnreadMessages(Long roomId, LocalDateTime after, Long userId) {
        return messageRepository.countUnreadMessages(roomId, after, userId);
    }

    @Override
    public List<MessageResponse> getMessagesSentAtAfter(Long roomId, LocalDateTime after) {
        return messageRepository.findByRoomIdAndSentAtAfter(roomId, after)
                .stream().map(this::toResponse).toList();
    }
}