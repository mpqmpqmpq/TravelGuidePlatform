package com.mpq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ统一配置类
 * 整合业务：文章审核、阅读量更新、聊天消息
 * 统一管理：全局死信、消息转换器、重试机制
 */
@Slf4j
@Configuration
public class RabbitMQConfig {
    //1. 业务队列/交换机常量
    // 文章审核业务
    public static final String REVIEW_QUEUE = "article.review.queue";
    public static final String RESULT_EXCHANGE = "article.result.exchange";
    public static final String RESULT_ROUTING_KEY = "article.result.#";
    //  阅读量更新业务
    public static final String VIEW_COUNT_QUEUE = "article.view.count.queue";
    public static final String VIEW_COUNT_EXCHANGE = "article.view.exchange";
    public static final String VIEW_COUNT_ROUTING_KEY = "article.view.increment";
    // 聊天消息业务
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String CHAT_MESSAGE_EXCHANGE = "chat.message.exchange";
    public static final String CHAT_MESSAGE_ROUTING_KEY = "chat.message.route";

    // 作用：调度器发送信号到此队列，消费者监听后执行批量入库，确保 basicAck 在消费者线程执行。
    public static final String INTERNAL_FLUSH_QUEUE = "internal.flush.queue";
    public static final String INTERNAL_FLUSH_ROUTING_KEY = "internal.flush";

    //  全局死信（统一管理所有失败消息）
    public static final String DLX_EXCHANGE = "article.dlx.exchange";
    public static final String DLX_QUEUE = "article.dlx.queue";
    public static final String DLX_ROUTING_KEY = "article.dlx.#";

    //日常行为日志
    public static final String BEHAVIOR_LOG_QUEUE = "behavior.log.queue";
    public static final String BEHAVIOR_LOG_EXCHANGE = "behavior.log.exchange";
    public static final String BEHAVIOR_LOG_ROUTING_KEY = "behavior.log.route";
    public static final String DLX_BEHAVIOR_LOG_ROUTING_KEY = "article.dlx.behavior";


    // 2. 队列声明

    /**
     * 审核队列（带死信绑定）
     */
    @Bean
    public Queue reviewQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE); // 绑定全局死信
        args.put("x-dead-letter-routing-key", "article.dlx.review"); // 审核死信路由键（唯一标识）
        args.put("x-message-ttl", 60000); // 消息60秒过期
        args.put("x-queue-mode", "lazy"); // 惰性队列：适合消息量大、消费慢的场景

        return QueueBuilder.durable(REVIEW_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * 阅读量队列（原有缺失的队列声明）
     */
    @Bean
    public Queue viewCountQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE); // 绑定全局死信
        args.put("x-dead-letter-routing-key", "article.dlx.view"); // 阅读量死信路由键
        args.put("x-message-ttl", 60000); // 60秒过期

        return QueueBuilder.durable(VIEW_COUNT_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * 聊天消息队列
     */
    @Bean
    public Queue chatMessageQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE); // 绑定全局死信
        args.put("x-dead-letter-routing-key", "article.dlx.chat"); // 聊天死信路由键
        args.put("x-message-ttl", 3600000); // 1小时过期
        args.put("x-queue-mode", "lazy"); // 惰性队列：聊天消息可能峰值高，避免内存溢出

        return QueueBuilder.durable(CHAT_MESSAGE_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * 内部刷新队列（持久化，用于存放刷新信号）
     */
    @Bean
    public Queue internalFlushQueue() {
        return QueueBuilder.durable(INTERNAL_FLUSH_QUEUE).build();
    }


    /**
     * 行为日志队列
     */
    @Bean
    public Queue  behaviorLogQueue(){
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange",DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key",DLX_BEHAVIOR_LOG_ROUTING_KEY);
        return QueueBuilder.durable(BEHAVIOR_LOG_QUEUE).build();

    }

    /**
     * 全局死信队列（存储所有业务的失败消息）
     */
    @Bean
    public Queue dlxQueue() {
        // 死信队列设置7天过期，便于人工排查后处理
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 604800000); // 7天过期（单位：毫秒）

        return QueueBuilder.durable(DLX_QUEUE)
                .withArguments(args)
                .build();
    }




    // 3. 交换机声明

    /**
     * 审核结果交换机
     */
    @Bean
    public TopicExchange resultExchange() {
        return ExchangeBuilder.topicExchange(RESULT_EXCHANGE)
                .durable(true) // 持久化：重启后交换机不丢失
                .build();
    }

    /**
     * 阅读量交换机
     */
    @Bean
    public TopicExchange viewCountExchange() {
        return ExchangeBuilder.topicExchange(VIEW_COUNT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 聊天消息交换机
     */
    @Bean
    public TopicExchange chatMessageExchange() {
        return ExchangeBuilder.topicExchange(CHAT_MESSAGE_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 行为日志交换机
     */
    @Bean
    public TopicExchange behaviorLogExchange(){
        return ExchangeBuilder.topicExchange(BEHAVIOR_LOG_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 全局死信交换机
     */
    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }


    //  4. 队列与交换机绑定

    /**
     * 审核队列绑定到审核交换机
     */
    @Bean
    public Binding reviewQueueBinding() {
        return BindingBuilder.bind(reviewQueue())
                .to(resultExchange())
                .with(RESULT_ROUTING_KEY);
    }

    /**
     *阅读量队列绑定到阅读量交换机
     */
    @Bean
    public Binding viewCountQueueBinding() {
        return BindingBuilder.bind(viewCountQueue())
                .to(viewCountExchange())
                .with(VIEW_COUNT_ROUTING_KEY);
    }

    /**
     * 聊天队列绑定到聊天交换机
     */
    @Bean
    public Binding chatMessageBinding() {
        return BindingBuilder.bind(chatMessageQueue())
                .to(chatMessageExchange())
                .with(CHAT_MESSAGE_ROUTING_KEY);
    }

    /**
     * 绑定内部刷新队列到已有的聊天消息交换机，使用独立路由键。
     */
    @Bean
    public Binding internalFlushBinding() {
        return BindingBuilder.bind(internalFlushQueue())
                .to(chatMessageExchange())
                .with(INTERNAL_FLUSH_ROUTING_KEY);
    }


    /**
     *行为日志队列绑定交换机
     */
    @Bean
    public  Binding behavioLogBinding(){
        return BindingBuilder.bind(behaviorLogQueue())
                .to(behaviorLogExchange())
                .with(BEHAVIOR_LOG_ROUTING_KEY);
    }

    /**
     * 死信队列绑定到死信交换机
     */
    @Bean
    public Binding dlxQueueBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }


    // 5. 通用组件配置

    /**
     *  JSON消息转换器（全局唯一，Java对象与JSON自动转换）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate（全局消息发送模板，确保可靠传输）
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter()); // 绑定转换器
        rabbitTemplate.setMandatory(true); // 强制路由：未找到队列时触发returns回调

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String correlationId = (correlationData != null && correlationData.getId() != null)
                    ? correlationData.getId()
                    : "unknown-" + System.currentTimeMillis();
            if (ack) {
                log.debug("消息成功到达交换机，correlationId: {}", correlationId);
            } else {
                log.error("消息未到达交换机，correlationId: {}, 原因: {}", correlationId, cause);
            }
        });

        // 消息路由失败回调
        rabbitTemplate.setReturnsCallback(returned -> {
            try {
                String body = new String(returned.getMessage().getBody(), StandardCharsets.UTF_8);
                log.error("消息路由失败：Exchange={}, RoutingKey={}, 错误码={}, 原因={}, 消息内容={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText(), body);
            } catch (Exception e) {
                log.error("解析路由失败消息体时出错", e);
            }
        });

        return rabbitTemplate;
    }

    /**
     * 重试拦截器（全局消费重试策略，避免瞬时错误导致消息丢失）
     */
    @Bean
    public StatefulRetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateful()
                .maxAttempts(3) // 总尝试次数：1次初始消费 + 2次重试
                .backOffOptions(1000, 2.0, 5000) // 退避策略：1s → 2s → 4s（最大5s）
                .recoverer(messageRecoverer()) // 重试耗尽后处理
                .build();
    }

    /**
     *消息恢复器（重试耗尽后拒绝消息，转发到死信队列）
     */
    @Bean
    public MessageRecoverer messageRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }

    /**
     * 消费者容器工厂（全局消费配置，控制消费并发和ACK）
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        factory.setMessageConverter(jsonMessageConverter()); // 绑定转换器
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 手动ACK：确保业务处理完成后再确认
        factory.setAdviceChain(retryInterceptor()); // 绑定重试拦截器
        factory.setPrefetchCount(20); // 预取数：1个消费者同时处理5条消息
        factory.setConcurrentConsumers(2); // 最小消费者数：默认2个线程消费
        factory.setMaxConcurrentConsumers(10); // 最大消费者数：峰值时自动扩容到10个线程
        factory.setIdleEventInterval(60000L); // 空闲检测：60秒无消息时触发空闲事件
        factory.setMissingQueuesFatal(false); // 队列不存在时不致命：避免服务启动失败

        return factory;
    }
}