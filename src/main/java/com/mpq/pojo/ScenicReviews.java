package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 景点评论表
 * </p>
 *
 * @author mpq
 * @since 2026-03-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scenic_reviews")
public class ScenicReviews implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Integer reviewId;

    /**
     * 景点ID
     */
    @TableField("scenic_id")
    private Integer scenicId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 评分（1-5星）
     */
    @TableField("rating")
    private BigDecimal rating;

    /**
     * 评论内容
     */
    @TableField("content")
    private String content;

    /**
     * 评论图片URL列表
     */
    @TableField("images")
    private String images;

    /**
     * 状态：1=正常,2=待审核,3=违规,4=已删除
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 回复的目标评论ID（支持嵌套评论）
     */
    @TableField("reply_to")
    private Long replyTo;


}
