package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.mpq.scheduled.RecommendationEffectsScheduler;
import lombok.Data;

/**
 * 记录推荐系统的曝光、点击、收藏数据，用于分析推荐效果和CTR
 * 数据来源：
 * - 曝光记录：由推荐服务在返回推荐结果时批量保存（每天仅保存一次）
 * - 点击/收藏：通过T+1批处理从user_behavior_log更新
 *
 * @author mpq
 * @since 2025-10-19
 * @see RecommendationEffectsScheduler
 */
@Data
@TableName("recommendation_effects")
public class RecommendationEffects implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 效果记录唯一标识
     */
    @TableId(value = "effect_id", type = IdType.AUTO)
    private Long effectId;

    /**
     * 用户ID（关联users表）
     */
    private Long userId;

    /**
     * 推荐的文章ID（关联articles表）
     */
    private Long articleId;

    /**
     * 召回来源（1=热门召回,2=分类召回,3=标签召回,4=行为召回）
     */
    private Integer recallSource;

    /**
     * 推荐位置（如第1条=1，第5条=5）
     */
    private Integer recPosition;

    /**
     * 是否点击（1=点击，0=未点击）
     */
    private Boolean isClick;

    /**
     * 是否收藏（1=收藏，0=未收藏）
     */
    private Boolean isCollect;

    /**
     * 推荐展示时间（用户看到推荐的时间）
     */
    private LocalDateTime showTime;

    /**
     * 点击时间（未点击则为NULL，通过T+1批处理更新）
     */
    private LocalDateTime clickTime;


}
