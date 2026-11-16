package com.mpq.manager;

import com.mpq.constants.HotArticleConstants;
import com.mpq.pojo.Article;
import com.mpq.utils.HotArticleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotArticleCacheManager {
    private final RedisTemplate<String, Object> redisTemplate;
    private final HotArticleUtil hotArticleUtil;
    /**
     * 处理热点数据
     */
    public void manageHotCache(Article article){
        try {
            Integer maxSize = (Integer) redisTemplate.opsForValue().get(HotArticleConstants.MAX_HOT_SIZE_KEY);
            if (maxSize == null) {
                maxSize = HotArticleConstants.DEFAULT_MAX_HOT_SIZE;
            }
            //判定
            if (hotArticleUtil.isHotArticle(article.getArticleId())) {
                String cacheKey = String.format(HotArticleConstants.HOT_CACHE_KEY, article.getArticleId());
                redisTemplate.opsForValue().set(cacheKey, article, HotArticleConstants.HOT_CACHE_TTL, TimeUnit.MINUTES);
                redisTemplate.opsForSet().add(HotArticleConstants.HOT_ID_SET_KEY, article.getArticleId());

                //缓存超量：淘汰热度低的
                Long currentSize = redisTemplate.opsForSet().size(HotArticleConstants.HOT_ID_SET_KEY);
                if (null != currentSize && currentSize > maxSize )  {
                    evictLowesScoreArticle();
                }
            } else {
                removeFromHotCache(article.getArticleId());
            }
        }catch (Exception e){

            log.error("处理热点数据异常",e);
        }
    }

    /**
     * 淘汰热度低的文章
     */
    private void evictLowesScoreArticle(){
        try {
            Set<Object> hotIds = redisTemplate.opsForSet().members(HotArticleConstants.HOT_ID_SET_KEY);
            if (hotIds == null || hotIds.isEmpty()) {
                return;
            }
            Integer lowestTd = null;
            double lowestScore = Double.MAX_VALUE;
            for (Object hotId : hotIds) {
                Integer articleId = (Integer) hotId;
                double score = hotArticleUtil.calculateHotScore(articleId);
                if (score < lowestScore) {
                    lowestScore = score;
                    lowestTd = articleId;
                }
            }
            if (lowestTd != null) {
                removeFromHotCache(lowestTd);
                log.info("热点缓存超量，淘汰低热度文章ID：{}，热度分：{}", lowestTd, lowestScore);
            }
        }
        catch (Exception e){

            log.error("淘汰热点数据异常",e);
        }
    }
    private void removeFromHotCache(Integer articleId){
        try {
            String cacheKey = String.format(HotArticleConstants.HOT_CACHE_KEY, articleId);
            redisTemplate.delete(cacheKey);
            redisTemplate.opsForSet().remove(HotArticleConstants.HOT_ID_SET_KEY, articleId);
        }
        catch (Exception e){

            log.error("移除热点数据异常",e);
        }
    }

}
