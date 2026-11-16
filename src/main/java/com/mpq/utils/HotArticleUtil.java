package com.mpq.utils;


import com.mpq.constants.HotArticleConstants;
import com.mpq.mapper.ArticleMapper;
import com.mpq.pojo.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Component
public class HotArticleUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    private final ArticleMapper articleMapper;

    /**
     * 实时更新阅读量
     */
    public void incrementViewCount(Integer articleId) {
        if (articleId == null) {
            return;
        }
        //累加阅读量
        String viewCountKey = String.format(HotArticleConstants.VIEW_COUNT_KEY, articleId);

        //首次访问时缓存发布时间
        String publishTimeKey = String.format(HotArticleConstants.PUBLISH_TIME_KEY, articleId);
        String likeCountKey = String.format(HotArticleConstants.LIKE_COUNT_KEY, articleId);
        if (!redisTemplate.hasKey(publishTimeKey)) {
            Article article = articleMapper.selectById(articleId);
            if (article != null) {
                long publishTimeStamp = article.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                redisTemplate.opsForValue().set(publishTimeKey, publishTimeStamp);
                redisTemplate.opsForValue().set(viewCountKey,article.getViewCount());
                redisTemplate.opsForValue().set(likeCountKey,article.getLikeCount());
            }
        }
        redisTemplate.opsForValue().increment(viewCountKey);
    }
    // 实时更新点赞量
    public void incrementLikeCount(Integer articleId, boolean isCancel) {
        if (articleId == null) {
            return;
        }
        //累加减点赞量
        String likeCountKey = String.format(HotArticleConstants.LIKE_COUNT_KEY, articleId);
        if (isCancel) {
            redisTemplate.opsForValue().decrement(likeCountKey);
        } else {
            redisTemplate.opsForValue().increment(likeCountKey);
        }
    }

    /**
     * 计算分数
     */
    public double calculateHotScore(Integer articleId) {
        Integer viewCount = (Integer) redisTemplate.opsForValue().get(String.format(HotArticleConstants.VIEW_COUNT_KEY, articleId));
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(String.format(HotArticleConstants.LIKE_COUNT_KEY, articleId));
        Long publishTime = (Long) redisTemplate.opsForValue().get(String.format(HotArticleConstants.PUBLISH_TIME_KEY, articleId));
        if (viewCount == null || likeCount == null || publishTime == null) {
            return 0;
        }
        double viewScore = Math.min(viewCount / 1000.0, 1.0);
        double likeScore = Math.min(likeCount / 100.0, 1.0);
        long publishDays = ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(publishTime),
                Instant.now()
        );
        double timeScore = Math.exp(-publishDays / 7.0);
        return (viewScore * 0.4 + likeScore * 0.3 + timeScore * 0.3) * 100;
    }

    /**
     * 判断是否是热点数据
     */
    public boolean isHotArticle(Integer articleId) {
        Integer threshold = (Integer) redisTemplate.opsForValue().get(HotArticleConstants.HOT_THRESHOLD_KEY);
        if (threshold == null || threshold == 0) {
            threshold = HotArticleConstants.DEFAULT_HOT_THRESHOLD;
        }

        double score = calculateHotScore(articleId);
        return score >= threshold;
    }
}
