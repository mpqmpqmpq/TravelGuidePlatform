package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follows")
@Schema(description = "用户关注关系")
public class UserFollow {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关注关系ID")
    private Integer followId;
    
    @Schema(description = "关注者ID", required = true)
    private Integer followerId;
    
    @Schema(description = "被关注者ID", required = true)
    private Integer followingId;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "关注时间")
    private LocalDateTime createdAt;
}