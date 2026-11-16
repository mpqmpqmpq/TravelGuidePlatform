package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message_read_status")
@Schema(description = "消息已读状态")
public class MessageReadStatus {
    @TableId(type = IdType.AUTO)
    @Schema(description = "消息ID")
    private Integer messageId;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "阅读时间")
    private LocalDateTime readAt;
}
