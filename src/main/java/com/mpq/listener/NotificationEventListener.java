package com.mpq.listener;

import com.mpq.pojo.Notification;
import com.mpq.service.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationPushService notificationPushService;
    
    @Async
    @EventListener
    public void handleNotificationEvent(Notification notification) {
        if (notification.getRecipientId().equals(notification.getSenderId())) return;
        notificationPushService.saveAndPush(notification);
    }
}