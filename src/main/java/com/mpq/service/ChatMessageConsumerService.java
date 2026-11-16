package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.config.RabbitMQConfig;
import com.mpq.mapper.MessageMapper;
import com.mpq.pojo.Message;
import com.mpq.pojo.PendingMessage;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageConsumerService extends ServiceImpl<MessageMapper, Message> {


    private final MessagesBatchAddService messagesBatchAddService;

    private final ConcurrentLinkedQueue<PendingMessage> buffer = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 20;


    /**
     * 监听聊天消息队列，先放入缓冲区，不立即确认
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void handleChatMessage(Message message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到消息: {}", message);
        buffer.offer(new PendingMessage(message, channel, deliveryTag));
        if (buffer.size() >= BATCH_SIZE) {
            messagesBatchAddService.processBatchMessages(buffer);
            log.info("已处理 {} 条消息", BATCH_SIZE);
        }
    }

    /**
     * 立即处理所有缓冲区中的消息
     */

    public void processAllBufferedMessages() {
        while (!buffer.isEmpty()) {
            messagesBatchAddService.processBatchMessages(buffer);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.INTERNAL_FLUSH_QUEUE,
    containerFactory = "rabbitListenerContainerFactory")
    public void handleFlushMessage(String signal, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("收到刷新信号: {}", signal);
            if (!buffer.isEmpty()) {
                messagesBatchAddService.processBatchMessages(buffer);
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理刷新信号时发生异常，但消费者将继续运行", e);

        }
    }

    /**
     * 获取缓冲区中待处理的消息数量
     */
    public int getBufferedMessageCount() {
        return buffer.size();
    }
    @PreDestroy
    public void onShutdown() {
        log.info("服务关闭，插入剩余消息");
        processAllBufferedMessages();
    }

}