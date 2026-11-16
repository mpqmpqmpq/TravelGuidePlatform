package com.mpq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationWithUserVo {
    private Integer conversationId;
    private Integer user1Id;
    private Integer user2Id;
    private Integer lastMessageId;
    private LocalDateTime updatedAt;
    private Integer unreadCount1;
    private Integer unreadCount2;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessageContent;
}

