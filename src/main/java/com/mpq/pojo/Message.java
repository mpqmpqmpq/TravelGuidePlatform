package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
@Schema(description = "私信消息")
public class Message {
    @TableId(type = IdType.AUTO)
    @Schema(description = "消息ID")
    private Integer messageId;

    @Schema(description = "所属会话ID")
    private Integer conversationId;

    @Schema(description = "发送者ID")
    private Integer senderId;

    @Schema(description = "接收者ID")
    private Integer receiverId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息状态：0=正常,1=撤回,2=删除")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "请求唯一ID")
    private String requestId;
}
