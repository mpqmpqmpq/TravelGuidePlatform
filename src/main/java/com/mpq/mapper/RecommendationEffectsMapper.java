package com.mpq.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.RecommendationEffects;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 推荐效果反馈表 Mapper 接口
 * </p>
 * <p>
 * 提供批量更新点击/收藏状态、清理过期数据等方法，配合定时任务实现T+1批处理
 * </p>
 *
 * @author mpq
 * @since 2025-10-22
 * @see RecommendationEffectsScheduler
 */
@Mapper
public interface RecommendationEffectsMapper extends BaseMapper<RecommendationEffects> {

    /**
     * 批量更新点击时间
     * <p>
     * 根据user_behavior_log中指定时间范围的点击记录，批量更新recommendation_effects表的点击状态
     * </p>
     *
     * @param startTime  行为日志查询开始时间
     * @param endTime    行为日志查询结束时间
     * @param clickDate  曝光记录的日期（用于关联show_time）
     * @return 更新条数
     */
    int batchUpdateClickTime(@Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("clickDate") LocalDateTime clickDate);

    /**
     * 批量更新收藏时间
     * <p>
     * 根据user_behavior_log中指定时间范围的收藏记录，批量更新recommendation_effects表的收藏状态
     * </p>
     *
     * @param startTime    行为日志查询开始时间
     * @param endTime      行为日志查询结束时间
     * @param collectDate  曝光记录的日期（用于关联show_time）
     * @return 更新条数
     */
    int batchUpdateCollectTime(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("collectDate") LocalDateTime collectDate);

    /**
     * 删除指定时间之前的曝光记录
     * <p>
     * 用于数据清理，删除超过保留期限的推荐效果数据
     * </p>
     *
     * @param cutoffTime 删除截止时间（show_time < cutoffTime）
     * @return 删除条数
     */
    int deleteByShowTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
