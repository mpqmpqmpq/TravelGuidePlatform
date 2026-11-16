package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversations")
@Schema(description = "私信会话")
public class Conversation {
    @TableId(type = IdType.AUTO)
    @Schema(description = "会话ID")
    private Integer conversationId;

    @Schema(description = "用户1ID（较小ID）")
    private Integer user1Id;

    @Schema(description = "用户2ID（较大ID）")
    private Integer user2Id;

    @Schema(description = "最后一条消息ID")
    private Integer lastMessageId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "用户1未读数")
    private Integer unreadCount1;

    @Schema(description = "用户2未读数")
    private Integer unreadCount2;
}
