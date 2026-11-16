package com.mpq.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "article.dlx.queue")
    public void handleDeadLetterMessage(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 解析消息内容
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到死信队列消息：{}", body);

            // 将消息解析为 Map
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> reviewTask = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});

            Integer articleId = (Integer) reviewTask.get("articleId");
            String content = (String) reviewTask.get("content");

            // 通过 WebSocket 广播消息
            String notification = String.format("文章ID：%d 的审核失败，请尽快处理！", articleId);
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.info("已通过 WebSocket 发送通知：{}", notification);

            channel.basicAck(deliveryTag, false);
            log.info("已通过 WebSocket 发送通知：{}", notification);

        } catch (Exception e) {
            log.error("处理死信队列消息失败", e);
            try {
                // 拒绝消息，避免重复消费
                channel.basicNack(deliveryTag, false, false);
                log.error("死信队列消息处理失败，已拒绝消息");
            } catch (Exception ex) {
                log.error("死信队列消息拒绝失败", ex);
            }
        }
    }



}
