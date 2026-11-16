package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bookmarks")
@Schema(description = "文章收藏")
public class BookMarks {
    @TableId(value = "bookmark_id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "文章ID")
    private Integer articleId;

    @Schema(description = "收藏时间")
    private LocalDateTime createdAt;

}
