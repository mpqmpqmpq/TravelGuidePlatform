package com.mpq.handler;

import com.mpq.utils.JwtTokenUtil;
import com.mpq.manager.ChatConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class ChatWebSocketEventHandler {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ChatConnectionManager chatConnectionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("=== 聊天 WebSocket 连接事件触发 ===");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String authToken = headerAccessor.getFirstNativeHeader("Authorization");
        
        log.debug("Authorization header: {}", authToken);

        if (authToken != null && authToken.startsWith("Bearer ")) {
            String token = authToken.substring(7);
            try {
                log.debug("验证聊天用户 token: {}...", token.substring(0, Math.min(20, token.length())));

                // 提取用户信息
                String username = jwtTokenUtil.extractUsername(token);
                Integer userId = jwtTokenUtil.extractUserId(token);
                
                log.debug("提取的用户名: {}", username);
                log.debug("提取的用户ID: {}", userId);

                // 添加用户连接到管理器
                chatConnectionManager.addUserConnection(userId.toString(), headerAccessor.getSessionId());
                log.info("=== 聊天 WebSocket 连接成功: {} (ID: {}) ===", username, userId);
                
            } catch (Exception e) {
                log.error("聊天 WebSocket 连接验证失败: ", e);
            }
        } else {
            log.warn("没有找到 Authorization header 或格式不正确");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("=== 聊天 WebSocket 断开连接事件触发 ===");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // 从连接管理器中获取用户信息
        String sessionId = headerAccessor.getSessionId();
        String userId = chatConnectionManager.getUserIdBySessionId(sessionId);
        
        if (userId != null) {
            chatConnectionManager.removeUserConnection(userId);
            log.info("=== 聊天 WebSocket 连接断开: 用户ID: {} ===", userId);
        }
    }
}
