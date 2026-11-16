package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
@Schema(description = "系统通知")
public class Notification {
    @TableId(type = IdType.AUTO)
    @Schema(description = "通知ID")
    private Integer notifId;
    
    @Schema(description = "接收用户ID", required = true)
    private Integer recipientId;
    
    @Schema(description = "发送用户ID")
    private Integer senderId;
    
    @Schema(description = "目标类型（1=文章,2=评论,3=用户）", required = true)
    private Integer targetType;
    
    @Schema(description = "目标ID", required = true)
    private Integer targetId;
    
    @Schema(description = "操作类型（1=点赞,2=评论,3=关注,4=回复,5=私信,6=文章审核）", required = true)
    private Integer actionType;
    
    @Schema(description = "已读标记")
    private Boolean isRead;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "附加数据(JSON)")
    private String extraData;
}