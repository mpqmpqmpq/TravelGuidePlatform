package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.NotificationsMapper;
import com.mpq.pojo.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService extends ServiceImpl<NotificationsMapper, Notification> {
    private final SimpMessagingTemplate messagingTemplate;
    
    public void saveAndPush(Notification notification) {
        this.save(notification);
        String destination = "/queue/notification/" + notification.getRecipientId();
        log.info(" 推送通知给用户：{}", notification.getRecipientId());
        messagingTemplate.convertAndSend(destination, notification);
    }
}