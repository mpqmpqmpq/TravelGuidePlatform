package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contacts")
@Schema(description = "用户联系人")
public class Contact {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "最后互动时间")
    private LocalDateTime lastInteraction;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "添加时间")
    private LocalDateTime createdAt;
}
