package com.mpq.service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.RecommendationEffectsMapper;
import com.mpq.pojo.RecommendationEffects;
import com.mpq.service.RecommendationEffectsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 推荐效果反馈表 服务实现类
 * </p>
 *
 * @author mpq
 * @since 2025-10-22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationEffectsServiceImpl extends ServiceImpl<RecommendationEffectsMapper, RecommendationEffects> implements RecommendationEffectsService {


    @Override
    public boolean updateClickTime(Integer userId, Integer articleId, LocalDateTime clickTime) {
        LambdaQueryWrapper<RecommendationEffects> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendationEffects::getUserId, userId)
                .eq(RecommendationEffects::getArticleId, articleId)
                .eq(RecommendationEffects::getIsClick, true)
                .orderByDesc(RecommendationEffects::getShowTime)
                .last("limit 1");
        RecommendationEffects record = this.getOne(queryWrapper);
        if (record == null) {
            log.debug("用户{}点击文章{}时，没有找到对应的记录", userId, articleId);
            return false;
        }
        record.setIsClick(true);
        record.setClickTime(clickTime);
        return this.updateById(record);
    }

    @Override
    public void batchSaveExposures(List<RecommendationEffects> exposures) {
        if (exposures == null || exposures.isEmpty()) {
            return;
        }
        this.saveBatch(exposures);
        log.debug("批量保存曝光记录 {} 条", exposures.size());
    }

    @Override
    public RecommendationEffects getLatestExposure(Integer userId, Integer articleId) {
        LambdaQueryWrapper<RecommendationEffects> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendationEffects::getUserId, userId.longValue())
                .eq(RecommendationEffects::getArticleId, articleId.longValue())
                .orderByDesc(RecommendationEffects::getShowTime)
                .last("limit 1");
        return this.getOne(queryWrapper);
    }
}
