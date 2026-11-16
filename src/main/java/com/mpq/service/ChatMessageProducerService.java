package com.mpq.service;

import com.mpq.config.RabbitMQConfig;
import com.mpq.pojo.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageProducerService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送聊天消息到队列
     */
    public void sendChatMessage(Message message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_MESSAGE_EXCHANGE,
                    RabbitMQConfig.CHAT_MESSAGE_ROUTING_KEY,
                    message
            );
            log.info("聊天消息已发送到队列: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.error("发送聊天消息失败: messageId={}, error={}",
                    message.getMessageId(), e.getMessage(), e);
        }
    }
}
