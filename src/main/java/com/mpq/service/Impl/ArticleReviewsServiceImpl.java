package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.ArticleReviewMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.vo.ArticleReviewWithUserVO;
import com.mpq.service.ArticleReviewsService;
import com.mpq.service.ArticleService;
import com.mpq.utils.NotificationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleReviewsServiceImpl extends ServiceImpl<ArticleReviewMapper, ArticleReview> implements ArticleReviewsService {
    private final ArticleReviewMapper articleReviewMapper;
    private final ArticleService articleService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationHelper notificationHelper;

    @Override
    public Result getArticleReviews(Page<ArticleReview> page) {
        Page<ArticleReview> articleReviews = this.page(page);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", articleReviews.getRecords());
        resultMap.put("total", articleReviews.getTotal());
        resultMap.put("current", articleReviews.getCurrent());
        resultMap.put("pages", articleReviews.getPages());
        resultMap.put("size", articleReviews.getSize());
        return Result.success("200", resultMap);
    }

    @Override
    @Transactional
    public Result ReviewArticles(ArticleReview articleReview) {
        articleReview.setReviewedAt(LocalDateTime.now());
        boolean updateR = this.updateById(articleReview);
        Article article = new Article();
        article.setArticleId(articleReview.getArticleId());
        if (articleReview.getStatus() == 2) {
            article.setStatus(1);
        }
        if (articleReview.getStatus() == 3) {
            article.setStatus(4);
        }
        article.setUpdatedAt(LocalDateTime.now());
        boolean updateA = articleService.updateById(article);
        Article articleServiceById = articleService.getById(articleReview.getArticleId());
        if (updateA && updateR) {
            clearArticleCache();
            if (articleReview.getStatus() == 2) {
                notificationHelper.sendNotification(0, articleServiceById.getUserId(), 1, article.getArticleId(), 6, "攻略审核通过");
            } else {
                notificationHelper.sendNotification(0, articleServiceById.getUserId(), 1, article.getArticleId(), 6, "攻略审核未通过");
            }
            return Result.success("200", "更新成功");
        }
        return Result.failure(500, "更新失败");
    }

    @Override
    public Result getArticleReviewsWithUser(Page<ArticleReviewWithUserVO> page, Integer status) {
        Page<ArticleReviewWithUserVO> articleReviewsWithUser = articleReviewMapper.selectArticleReviewsWithUser(page, status);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", articleReviewsWithUser.getRecords());
        resultMap.put("total", articleReviewsWithUser.getTotal());
        resultMap.put("current", articleReviewsWithUser.getCurrent());
        resultMap.put("pages", articleReviewsWithUser.getPages());
        resultMap.put("size", articleReviewsWithUser.getSize());

        return Result.success("获取审批用户信息成功", resultMap);
    }

    @Override
    public ArticleReview getByArticleId(Integer articleId) {
        LambdaQueryWrapper<ArticleReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleReview::getArticleId, articleId);
        return this.getOne(queryWrapper);
    }

    private void clearArticleCache() {
        // 删除所有文章列表缓存
        Set<String> keys = redisTemplate.keys("articles:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
