package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.LikeMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.Comment;
import com.mpq.pojo.Like;
import com.mpq.pojo.vo.LikeStatusWithCountVo;
import com.mpq.pojo.vo.LikeWithArticleVo;
import com.mpq.service.ArticleService;
import com.mpq.service.CommentsService;
import com.mpq.service.LikeService;
import com.mpq.utils.HotArticleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {
    private final LikeMapper likeMapper;
    private final ArticleService articleService;
    private final CommentsService commentsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HotArticleUtil hotArticleUtil;

    private final static Integer LIKE_TYPE_ARTICLE = 1;
    private final static Integer LIKE_TYPE_COMMENT = 2;

    @Override
    public Result selectLikeWithArticle(Page<LikeWithArticleVo> page, Integer userId) {
        Page<LikeWithArticleVo> likeWithArticleVoPage = likeMapper.selectLikeWithArticle(page, userId);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", likeWithArticleVoPage.getRecords());
        resultMap.put("total", likeWithArticleVoPage.getTotal());
        resultMap.put("current", likeWithArticleVoPage.getCurrent());
        resultMap.put("pages", likeWithArticleVoPage.getPages());
        resultMap.put("size", likeWithArticleVoPage.getSize());
        return Result.success("获取成功", resultMap);
    }

    @Transactional
    @Override
    public boolean likeArticle(Integer articleId, Integer userId) {
        Like like = new Like();
        like.setTargetId(articleId);
        like.setUserId(userId);
        like.setTargetType(LIKE_TYPE_ARTICLE);
        like.setCreatedAt(LocalDateTime.now());
        Article article = articleService.getById(articleId);
        article.setLikeCount(article.getLikeCount() + 1);
        articleService.updateById(article);
        cacheClear(articleId, userId);
        hotArticleUtil.incrementLikeCount(articleId,false);
        return this.save(like);
    }

    @Transactional
    @Override
    public boolean cancelLikeArticle(Integer articleId, Integer userId) {
        Article article = articleService.getById(articleId);
        if (article.getLikeCount() > 0) {
            article.setLikeCount(article.getLikeCount() - 1);
        }
        articleService.updateById(article);
        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getTargetId, articleId)
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, LIKE_TYPE_ARTICLE);

        hotArticleUtil.incrementLikeCount(articleId,true);
        cacheClear(articleId, userId);
        return this.remove(queryWrapper);
    }

    @Transactional
    @Override
    public boolean likeComment(Integer commentId, Integer userId) {
        Like like = new Like();
        like.setTargetId(commentId);
        like.setUserId(userId);
        like.setTargetType(LIKE_TYPE_COMMENT);
        like.setCreatedAt(LocalDateTime.now());
        Comment comment = commentsService.getById(commentId);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentsService.updateById(comment);
        return this.save(like);
    }

    @Override
    public boolean cancelLikeComment(Integer commentId, Integer userId) {
        Comment comment = commentsService.getById(commentId);
        if (comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            commentsService.updateById(comment);
        }
        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getTargetId, commentId)
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, LIKE_TYPE_COMMENT);

        return this.remove(queryWrapper);
    }

    @Override
    public Result getArticleLikeStatus(Integer articleId, Integer userId) {
        String cacheKey = "article_like_status:" + articleId + ":" + userId;
        Object likeStatus = redisTemplate.opsForValue().get(cacheKey);
        if (likeStatus != null) {
            return Result.success("获取成功", (LikeStatusWithCountVo) likeStatus);
        }
        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getTargetId, articleId)
                .eq(Like::getUserId, userId);
        boolean isLike = this.exists(queryWrapper);
        LambdaQueryWrapper<Like> likeCountQueryWrapper = new LambdaQueryWrapper<>();
        likeCountQueryWrapper.eq(Like::getTargetId, articleId);
        long count = likeMapper.selectCount(likeCountQueryWrapper);
        LikeStatusWithCountVo likeStatusWithCountVo = new LikeStatusWithCountVo();
        likeStatusWithCountVo.setLike(isLike);
        likeStatusWithCountVo.setCount(count);
        Random random = new Random();
        long randomTime = random.nextInt(10) + 5;
        redisTemplate.opsForValue().set(cacheKey, likeStatusWithCountVo, randomTime, TimeUnit.MINUTES);
        return Result.success("获取成功", likeStatusWithCountVo);
    }

    @Override
    public void deleteByArticleId(Integer articleId) {
        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getTargetId, articleId);
        this.remove(queryWrapper);
    }

    private void cacheClear(Integer articleId, Integer userId) {
        String cacheKey = "article_like_status:" + articleId + ":" + userId;
        redisTemplate.delete(cacheKey);
    }
}
