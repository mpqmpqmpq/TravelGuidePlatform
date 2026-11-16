package com.mpq.consumer;

import com.mpq.config.RabbitMQConfig;
import com.mpq.pojo.dto.ViewCountMessageDto;
import com.mpq.service.ArticleService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 阅读量异步更新消费者（仅保留核心消费逻辑）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountConsumer {
    private final ArticleService articleService;
    private final RedisTemplate<String, Object> redisTemplate;
    // 幂等性缓存Key（24小时过期，避免重复消费）
    private static final String CACHE_KEY_MESSAGE = "article:view:msg:%s";

    @RabbitListener(queues = RabbitMQConfig.VIEW_COUNT_QUEUE)
    public void handleViewCount(ViewCountMessageDto message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            // 1. 幂等性校验：避免重复消费
            String msgCacheKey = String.format(CACHE_KEY_MESSAGE, message.getMessageId());
            Boolean isProcessed = redisTemplate.opsForValue().setIfAbsent(
                    msgCacheKey, "processed", 24, TimeUnit.HOURS
            );

            if (isProcessed == null || !isProcessed) {
                log.debug("消息已重复处理，直接确认：messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 核心业务：更新数据库阅读量
            articleService.batchUpdateViewCount(message.getArticleId(), message.getIncrement());

            // 3. 处理成功：确认消息
            channel.basicAck(deliveryTag, false);
            log.debug("阅读量更新成功：articleId={}，increment={}，messageId={}",
                    message.getArticleId(), message.getIncrement(), message.getMessageId());

        } catch (Exception e) {
            log.error("处理阅读量消息失败：articleId={}，messageId={}",
                    message.getArticleId(), message.getMessageId(), e);
            // 处理失败：拒绝消息，进入死信队列
            channel.basicReject(deliveryTag, false);
        }
    }
}