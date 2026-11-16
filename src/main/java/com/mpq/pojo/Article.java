package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("articles")
@Schema(description = "文章内容")
public class Article {
    @TableId(type = IdType.AUTO)
    @Schema(description = "文章ID")
    private Integer articleId;
    
    @Schema(description = "作者ID", required = true)
    private Integer userId;
    
    @Schema(description = "标题", required = true)
    private String title;
    
    @Schema(description = "内容(HTML/富文本)", required = true)
    private String content;
    
    @Schema(description = "封面图URL")
    private String coverImage;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "发布时间")
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
    
    @Schema(description = "浏览次数")
    private Integer viewCount;
    
    @Schema(description = "评论总数")
    private Integer commentCount;
    
    @Schema(description = "点赞总数")
    private Integer likeCount;
    
    @Schema(description = "状态（0=草稿,1=已发布,2=隐藏,3=待审核,4=不通过）")
    private Integer status;
    
    @Schema(description = "分类ID")
    private Integer categoryId;

    @TableField(exist = false) // 标记为非数据库字段
    private Double sortScore;
}
