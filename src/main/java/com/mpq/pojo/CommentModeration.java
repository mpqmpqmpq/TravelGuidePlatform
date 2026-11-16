package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_moderations")
@Schema(description = "评论审核记录")
public class CommentModeration {
    @TableId(type = IdType.AUTO)
    @Schema(description = "审核记录ID")
    private Long modId;
    
    @Schema(description = "被审核评论ID", required = true)
    private Long commentId;
    
    @Schema(description = "审核员ID")
    private Long moderatorId;
    
    @Schema(description = "操作类型（1=通过,2=删除,3=修改）", required = true)
    private Integer action;
    
    @Schema(description = "操作原因")
    private String reason;
    
    @Schema(description = "原始内容")
    private String originalContent;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "审核时间")
    private LocalDateTime createdAt;
}