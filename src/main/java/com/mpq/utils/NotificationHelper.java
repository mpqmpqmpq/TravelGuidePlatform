package com.mpq.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpq.pojo.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationHelper {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendNotification(Integer senderId, Integer recipientId, Integer targetType,
                                 Integer targetId, Integer actionType, String extraData) {
        if (recipientId == null || recipientId.equals(senderId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setSenderId(senderId);
        notification.setRecipientId(recipientId);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setActionType(actionType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notification.setExtraData(extraData != null ?
                    objectMapper.writeValueAsString(Map.of("content", extraData)) : null);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            notification.setExtraData(null);
        }
        eventPublisher.publishEvent(notification);
        log.info("通知已发布: 接收者={}, 类型={}, 动作={}", recipientId, targetType, actionType);
    }

    public void sendPrivateMessageNotification(Integer senderId, Integer recipientId,
                                               Integer messageId, String content) {
        sendNotification(senderId, recipientId, 3, messageId, 5, content);
    }
}