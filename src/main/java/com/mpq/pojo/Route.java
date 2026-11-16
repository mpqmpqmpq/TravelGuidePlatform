package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 旅行路线表
 * </p>
 *
 * @author mpq
 * @since 2026-03-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("route")
public class Route implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 创建用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 路线标题
     */
    @TableField("title")
    private String title;

    /**
     * 路线描述
     */
    @TableField("description")
    private String description;

    /**
     * 封面图
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 天数
     */
    @TableField("days")
    private Integer days;

    /**
     * 状态：1=公开，2=私密
     */
    @TableField("status")
    private Integer status;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
