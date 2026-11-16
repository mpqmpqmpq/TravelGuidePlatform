package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("comments")
@Schema(description = "文章评论")
public class Comment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "评论ID")
    private Integer commentId;
    
    @Schema(description = "评论者ID", required = true)
    private Integer userId;
    
    @Schema(description = "文章ID", required = true)
    private Integer articleId;
    
    @Schema(description = "父评论ID")
    private Integer parentId;
    
    @Schema(description = "评论内容", required = true)
    private String content;
    
    @Schema(description = "嵌套深度（0=顶级评论）")
    private Integer depth;
    
    @Schema(description = "评论路径（格式：/1/3/5/）")
    private String path;
    
    @Schema(description = "点赞数")
    private Integer likeCount;
    
    @Schema(description = "回复数")
    private Integer replyCount;
    
    @Schema(description = "删除标记（1=已删除）")
    private Boolean isDeleted;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "评论时间")
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "最后编辑时间")
    private LocalDateTime updatedAt;

    @Schema(description = "审核状态（0=待审核,1=已发布,2=违规）")
    private Integer status;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户头像")
    private String userAvatar;

    // 新增字段：子评论
    @TableField(exist = false)
    private List<Comment> children;


}
