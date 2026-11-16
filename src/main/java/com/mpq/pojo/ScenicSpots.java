package com.mpq.pojo;

import java.math.BigDecimal;

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
 * 景点信息表
 * </p>
 *
 * @author mpq
 * @since 2026-03-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scenic_spots")
public class ScenicSpots implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 景点唯一标识
     */
    @TableId(value = "scenic_id", type = IdType.AUTO)
    private Integer scenicId;

    /**
     * 景点名称
     */
    @TableField("name")
    private String name;

    /**
     * 景点简介/描述
     */
    @TableField("description")
    private String description;

    /**
     * 地理位置（文字描述）
     */
    @TableField("location")
    private String location;

    /**
     * 经度
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 门票价格（0免费）
     */
    @TableField("ticket_price")
    private BigDecimal ticketPrice;

    /**
     * 开放时间（如08:00-18:00）
     */
    @TableField("open_hours")
    private String openHours;

    /**
     * 封面图片URL
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 平均评分（1-5分）
     */
    @TableField("avg_score")
    private BigDecimal avgScore;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 收藏数
     */
    @TableField("favorite_count")
    private Integer favoriteCount;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 状态：1-正常，2-下架
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


}
