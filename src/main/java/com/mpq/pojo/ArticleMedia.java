package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_media")
@Schema(description = "文章媒体资源表（支持图片、视频）")
public class ArticleMedia {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "媒体资源唯一标识")
    private Integer mediaId;
    
    @Schema(description = "关联文章ID", required = true)
    private Integer articleId;
    
    @Schema(description = "媒体文件URL", required = true)
    private String mediaUrl;
    
    @Schema(description = "媒体类型（image=图片, video=视频）", required = true)
    private String mediaType;
    
    @Schema(description = "在文章中的位置顺序")
    private Integer position;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "上传时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "媒体资源标题（视频名称/图片描述）")
    private String mediaTitle;
    
    @Schema(description = "视频时长（单位：秒，仅视频类型生效）")
    private Integer videoDuration;
    
    @Schema(description = "视频封面图URL（仅视频类型生效）")
    private String videoCoverUrl;
    
    @Schema(description = "资源状态（1=正常，2=禁用，3=待审核）")
    private Integer status;
    
    @Schema(description = "上传用户ID（关联users表）", required = true)
    private Integer uploadUserId;
    
    @Schema(description = "额外信息（JSON格式，如分辨率、文件大小）")
    private String extraInfo;
}
