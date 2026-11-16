package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.RecommendationEffects;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationEffectsService extends IService<RecommendationEffects>{
    boolean updateClickTime(Integer userId, Integer articleId, LocalDateTime clickTime);

    void batchSaveExposures(List<RecommendationEffects> exposures);

    RecommendationEffects getLatestExposure(Integer userId, Integer articleId);
}
