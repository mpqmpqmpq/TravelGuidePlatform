package com.mpq.listener;

import com.mpq.pojo.OnlineStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class OnlineStatusListener {
 
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
 
    @MessageMapping("/connect")
    public void handleConnect(String username) {
        messagingTemplate.convertAndSend("/topic/online",  OnlineStatus.builder()
            .username(username)
            .online(true)
            .build());
    }
 
    @MessageMapping("/disconnect")
    public void handleDisconnect(String username) {
        messagingTemplate.convertAndSend("/topic/online",  OnlineStatus.builder() 
            .username(username)
            .online(false)
            .build());
    }
}