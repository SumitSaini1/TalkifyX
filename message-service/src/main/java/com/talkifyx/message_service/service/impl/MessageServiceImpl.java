package com.talkifyx.message_service.service.impl;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.*;
import com.talkifyx.message_service.feign.AuthServiceClient;
import com.talkifyx.message_service.feign.RoomServiceClient;
import com.talkifyx.message_service.repository.MessageReactionRepository;
import com.talkifyx.message_service.repository.MessageRepository;
import com.talkifyx.message_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
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

        // Populate nested reply preview (one level only — no recursion)
        MessageResponse replyPreview = null;
        if (m.getReplyToMessageId() != null) {
            try {
                replyPreview = messageRepository.findById(m.getReplyToMessageId())
                        .map(orig -> {
                            String origName = null;
                            try {
                                ApiResponse<SenderDto> origRes = authServiceClient.getUserById(orig.getSenderId());
                                if (origRes != null && origRes.getData() != null)
                                    origName = origRes.getData().getFullName();
                            } catch (Exception ignored) {}
                            return MessageResponse.builder()
                                    .messageId(orig.getMessageId())
                                    .roomId(orig.getRoomId())
                                    .senderId(orig.getSenderId())
                                    .senderName(origName)
                                    .content(orig.isDeleted() ? "This message was deleted" : orig.getContent())
                                    .type(orig.getType())
                                    .mediaUrl(orig.getMediaUrl())
                                    .isDeleted(orig.isDeleted())
                                    .sentAt(orig.getSentAt())
                                    .build();
                        })
                        .orElse(null);
            } catch (Exception ignored) {}
        }

        // Build reaction groups
        List<ReactionGroupDto> reactionGroups = buildReactionGroups(m.getMessageId());

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
                .replyToMessage(replyPreview)
                .isEdited(m.isEdited())
                .isDeleted(m.isDeleted())
                .deliveryStatus(m.getDeliveryStatus())
                .sentAt(m.getSentAt())
                .editedAt(m.getEditedAt())
                .reactions(reactionGroups)
                .build();
    }

    private List<ReactionGroupDto> buildReactionGroups(String messageId) {
        return reactionRepository.findByMessageId(messageId)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getEmoji()))
                .entrySet().stream()
                .map(e -> ReactionGroupDto.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .userIds(e.getValue().stream().map(r -> r.getUserId()).toList())
                        .build())
                .collect(Collectors.toList());
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
    public PagedResponse<MessageResponse> getMessagesByRoom(Long roomId, int page, int size, Long userId) {
        Page<Message> result = messageRepository
                .findVisibleMessagesByRoomId(roomId, userId, PageRequest.of(page, size));
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
    public List<MessageResponse> getMessagesBefore(Long roomId, LocalDateTime before, Long userId) {
        return messageRepository.findVisibleMessagesByRoomIdAndSentAtBefore(roomId, before, userId)
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
    @Transactional
    public void deleteMessage(String messageId, Long senderId, String deleteType) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if ("ME".equalsIgnoreCase(deleteType)) {
            m.getDeletedForUsers().add(senderId);
            messageRepository.save(m);
        } else {
            // Delete for EVERYONE
            if (!m.getSenderId().equals(senderId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your message");
            m.setDeleted(true);
            m.setContent("This message was deleted");
            m.setMediaUrl(null);
            messageRepository.save(m);
        }
    }

    @Override
    public List<MessageResponse> searchMessages(Long roomId, String keyword, Long userId) {
        return messageRepository.searchInRoom(roomId, keyword, userId)
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
    public List<MessageResponse> getMessagesSentAtAfter(Long roomId, LocalDateTime after, Long userId) {
        return messageRepository.findVisibleMessagesByRoomIdAndSentAtAfter(roomId, after, userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void markRoomMessagesRead(Long roomId, Long readerId) {
        messageRepository.bulkUpdateDeliveryStatus(roomId, readerId, DeliveryStatus.READ);
    }

    @Override
    @Transactional
    public List<ReactionGroupDto> reactToMessage(String messageId, Long userId, String emoji) {
        // Ensure message exists
        messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        var existing = reactionRepository.findByMessageIdAndUserId(messageId, userId);
        if (existing.isPresent()) {
            if (existing.get().getEmoji().equals(emoji)) {
                // Toggle OFF — same emoji clicked again
                reactionRepository.deleteByMessageIdAndUserId(messageId, userId);
            } else {
                // Switch emoji
                existing.get().setEmoji(emoji);
                reactionRepository.save(existing.get());
            }
        } else {
            // New reaction
            reactionRepository.save(MessageReaction.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .emoji(emoji)
                    .build());
        }
        return buildReactionGroups(messageId);
    }
}