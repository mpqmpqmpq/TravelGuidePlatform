package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("likes")
@Schema(description = "用户点赞记录")
public class Like {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "点赞记录ID")
    private Integer likeId;
    
    @Schema(description = "点赞用户ID")
    private Integer userId;
    
    @Schema(description = "目标ID（文章ID/评论ID）")
    private Integer targetId;
    
    @Schema(description = "点赞时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "目标类型（1：文章 2：评论）")
    private Integer targetType;
}
