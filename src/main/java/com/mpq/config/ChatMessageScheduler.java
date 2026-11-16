package com.mpq.config;

import com.mpq.service.ChatMessageConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ChatMessageScheduler {
    
    private final ChatMessageConsumerService chatMessageConsumerService;
    private final RabbitTemplate rabbitTemplate;
    /**
     * 定时检查并处理缓冲区中的消息
     * 每5秒执行一次，确保即使消息数量不足20条也能及时入库
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingMessages() {
        int bufferedCount = chatMessageConsumerService.getBufferedMessageCount();
        log.info("当前缓冲区消息数：{}", bufferedCount);
        if (bufferedCount > 0) {
            log.info("发现{}条待处理消息，开始批量处理", bufferedCount);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_MESSAGE_EXCHANGE,
                    RabbitMQConfig.INTERNAL_FLUSH_ROUTING_KEY,
                    "FLUSH_"+System.currentTimeMillis()
                    );
        }
    }
}
