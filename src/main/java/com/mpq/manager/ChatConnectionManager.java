package com.mpq.manager;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatConnectionManager {
    
    // 用户ID到会话ID的映射
    private final ConcurrentHashMap<String, String> userSessionMap = new ConcurrentHashMap<>();
    
    // 会话ID到用户ID的映射
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public void addUserConnection(String userId, String sessionId) {
        userSessionMap.put(userId, sessionId);
        sessionUserMap.put(sessionId, userId);
    }

    public void removeUserConnection(String userId) {
        String sessionId = userSessionMap.get(userId);
        if (sessionId != null) {
            userSessionMap.remove(userId);
            sessionUserMap.remove(sessionId);
        }
    }

    public String getSessionIdByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    public String getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public boolean isUserOnline(String userId) {
        return userSessionMap.containsKey(userId);
    }
}
