package com.mpq.scheduled;

import com.mpq.mapper.RecommendationEffectsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 推荐效果定时任务
 * 负责推荐效果数据的批量更新和数据清理，避免实时更新带来的行锁问题
 * 任务说明：
 * 1. 凌晨2点：批量更新前一天的点击和收藏状态（T+1批处理）
 * 2. 凌晨3点：清理90天前的过期数据，避免数据爆炸
 *
 *
 * @author mpq
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEffectsScheduler {

    private final RecommendationEffectsMapper recommendationEffectsMapper;

    /**
     * 批量更新点击和收藏状态
     * 每天凌晨2点执行，基于user_behavior_log中前一天的点击(action_type=2)和收藏(action_type=3)记录，
     * 批量更新recommendation_effects表中的is_click和is_collect字段
     * 优点：避免每次点击都实时UPDATE，减少行锁竞争，适合大数据量场景
     *
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void batchUpdateClickAndCollect() {
        log.info("开始执行推荐效果批处理更新任务");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.plusDays(1).atStartOfDay();

        // 批量更新点击状态（基于 user_behavior_log 中前一天的点击记录）
        int updatedCount = recommendationEffectsMapper.batchUpdateClickTime(
                startOfYesterday, endOfYesterday, yesterday.atStartOfDay()
        );
        log.info("推荐效果点击状态更新完成，更新条数: {}", updatedCount);

        // 批量更新收藏状态
        int collectUpdatedCount = recommendationEffectsMapper.batchUpdateCollectTime(
                startOfYesterday, endOfYesterday, yesterday.atStartOfDay()
        );
        log.info("推荐效果收藏状态更新完成，更新条数: {}", collectUpdatedCount);
    }

    /**
     * 清理过期数据
     * 每天凌晨3点执行，删除90天前的推荐效果数据，避免数据库存储爆炸
     * 注意：生产环境建议使用分区表+DROP PARTITION，DELETE会产生大量binlog
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldData() {
        log.info("开始执行推荐效果数据清理任务");

        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        LocalDateTime cutoffDateTime = cutoffDate.atStartOfDay();

        int deletedCount = recommendationEffectsMapper.deleteByShowTimeBefore(cutoffDateTime);
        log.info("推荐效果数据清理完成，删除 {} 天前的数据，删除条数: {}", 90, deletedCount);
    }
}