package com.mpq.controller;

import com.mpq.pojo.Conversation;
import com.mpq.pojo.Message;
import com.mpq.service.ChatMessageProducerService;
import com.mpq.service.MessageService;
import com.mpq.service.UserChatService;
import com.mpq.utils.NotificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Controller
@Slf4j
public class WebSocketController {
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMessageProducerService chatMessageProducerService;
    private final UserChatService userChatService;


    private final NotificationHelper notificationHelper;

    @MessageMapping("/chat/send")
    public void sendMessage(Message message, Principal principal) {
        Conversation orCreateConversation = userChatService.
                getORCreateConversation(message.getSenderId(), message.getReceiverId());
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus(0);
        message.setConversationId(orCreateConversation.getConversationId());
        message.setRequestId(UUID.randomUUID().toString());
        try {
            messageService.updateMessageCacheOnSend(message);
        } catch (Exception e) {
            log.warn("更新消息缓存失败：{}", e.getMessage());
        }
        chatMessageProducerService.sendChatMessage(message);
        String destination = "/queue/private/" + message.getReceiverId();

        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("推送消息失败：{}", e.getMessage(), e);
        }

        notificationHelper.sendPrivateMessageNotification(
                message.getSenderId(),
                message.getReceiverId(),
                message.getMessageId(),
                message.getContent());
        log.info("通知已发布给用户：{}", message.getReceiverId());
    }
}
