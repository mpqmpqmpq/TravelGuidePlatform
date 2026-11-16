package com.mpq.pojo.vo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Integer notifId;
    private Integer recipientId;
    private Integer senderId;
    private String senderUsername;
    private String senderAvatar;
    private Integer targetType;
    private Integer targetId;
    private Integer actionType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String extraData;
}