package com.mpq.config;

import com.mpq.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 导入Slf4j
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.Collections;

@RequiredArgsConstructor
@Slf4j // 添加Slf4j注解
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtTokenUtil jwtTokenUtil;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(heartBeatScheduler());
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(64 * 1024);
        registry.setSendBufferSizeLimit(2 * 1024 * 1024);
        registry.setSendTimeLimit(20_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        String jwtToken = token.substring(7);
                        try {
                            // 使用 JwtTokenUtil 验证token并提取用户信息
                            String username = jwtTokenUtil.extractUsername(jwtToken);
                            Integer userId = jwtTokenUtil.extractUserId(jwtToken);

                            if (username != null && !jwtTokenUtil.isTokenExpired(jwtToken)) {
                                // 创建认证对象并绑定到会话
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                                accessor.setUser(authentication);
                                log.info("用户 {} 认证成功，用户ID: {}", username, userId);
                            }
                        } catch (Exception e) {
                            log.warn("JWT认证失败: {}", e.getMessage());
                        }
                    }
                    log.info("STOMP CONNECT message received: Session ID = {}", accessor.getSessionId());
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    log.info("STOMP SEND message received: Destination = {}, Session ID = {}",
                            accessor.getDestination(), accessor.getSessionId());
                }
                return message;
            }
        });
    }

}