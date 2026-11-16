package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.config.RabbitMQConfig;
import com.mpq.constants.HotArticleConstants;
import com.mpq.manager.HotArticleCacheManager;
import com.mpq.mapper.ArticleMapper;
import com.mpq.mapper.ArticleReviewMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.dto.ViewCountMessageDto;
import com.mpq.service.ArticleRecommendService;
import com.mpq.service.ArticleService;
import com.mpq.service.BookMarksService;
import com.mpq.utils.HotArticleUtil;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    private final ArticleMapper articleMapper;
    private final ArticleReviewMapper articleReviewMapper;
    private final BookMarksService bookMarksService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final HotArticleUtil hotArticleUtil;
    private final HotArticleCacheManager hotArticleCacheManager;
    private final ArticleRecommendService articleRecommendService;

    // 状态常量
    private final static Integer STATUS_REVIEW = 2;
    private final static Integer STATUS_PUBLISHED = 1;
    private final static Integer STATUS_TO_BE_REVIEWED = 3;
    private final static Integer STATUS_REJECT = 4;

    // 缓存Key常量
    private final static String CACHE_KEY_DETAIL = "article:detail:%d"; // 文章详情
    private final static String CACHE_KEY_HOT = "article:hot:top5"; // 热门缓存

    @Override
    @Transactional
    public Article publishArticles(Article article) {
        articleMapper.updateById(article);
        // 新增审核记录
        ArticleReview articleReview = new ArticleReview();
        articleReview.setArticleId(article.getArticleId());
        articleReview.setStatus(1);
        articleReview.setCreatedAt(LocalDateTime.now());
        articleReviewMapper.insert(articleReview);
        String cacheKey = "article:user:" + article.getUserId() + ":page:*";
        Set<String> keys = redisTemplate.keys(cacheKey);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("删除缓存");
        return article;
    }

    /**
     * 获取用户文章列表
     *
     * @param page
     * @param userId
     * @param status
     * @return
     */
    @Override
    public Page<Article> getPageByUserId(Page<Article> page, int userId, Integer status) {
        // 根据status构建缓存key
        String cacheKey = "article:user:" + userId + ":page:" + page.getCurrent() + ":size:" + page.getSize() + ":status:" + (status == null ? "all" : status);
        Page<Article> cachePage = getArticleCache(cacheKey);
        if (cachePage != null) {
            return cachePage;
        }


        LambdaQueryWrapper<Article> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(Article::getUserId, userId)
                .isNull(Article::getTitle);
        this.remove(delWrapper);

        // 数据库查询（按创建时间倒序）
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId);
        // 添加status筛选条件
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        wrapper.orderByDesc(Article::getCreatedAt);
        Page<Article> result = this.page(page, wrapper);

        if (result.getTotal() > 0)
            redisTemplate.opsForValue().set(cacheKey, result, 30 + new Random().nextInt(5), TimeUnit.MINUTES);
        return result;
    }


    @Override
    public Page<Article> getArticlePageList(Page<Article> page) {

        return articleRecommendService.getPersonalRecommend((int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public Page<Article> getArticlePageListByCategoryId(Page<Article> articlePage, Integer categoryId) {
        String cacheKey = "article:category:" + categoryId + ":page" + articlePage.getCurrent() + ":size" + articlePage.getSize();
        Page<Article> cachePage = getArticleCache(cacheKey);
        if (cachePage != null) {
            return cachePage;
        }

        // 数据库查询
        Page<Article> result = lambdaQuery()
                .eq(Article::getCategoryId, categoryId)
                .eq(Article::getStatus, 1)
                .ne(Article::getUserId, UserContextHolder.getUserId())
                .orderByDesc(Article::getCreatedAt)
                .page(articlePage);

        if (result.getTotal() > 0)
            redisTemplate.opsForValue().set(cacheKey, result, 5 + new Random().nextInt(5), TimeUnit.MINUTES);
        return result;
    }


    @Transactional
    @Override
    public boolean updateArticleStatus(Article article) {
        if (article == null || article.getStatus() == null) {
            return false;
        }
        // 仅处理非“待审核”和“拒绝”的状态变更
        if (article.getStatus() != STATUS_TO_BE_REVIEWED && article.getStatus() != STATUS_REJECT) {
            bookMarksService.deleteBookMarksByArticleId(article.getArticleId());
            boolean success = this.updateById(article);
            // 状态变更后清理详情缓存
            if (success) {
                redisTemplate.delete(String.format(CACHE_KEY_DETAIL, article.getArticleId()));
            }
            return success;
        }
        return false;
    }


    @Override
    public void incrementViewCount(Integer articleId) {
        if (articleId == null) {
            log.warn("文章ID为空，跳过阅读量更新");
            return;
        }
        // 生成唯一消息ID
        String messageId = UUID.randomUUID().toString();
        ViewCountMessageDto message = new ViewCountMessageDto(articleId, 1, messageId);

        try {
            // 发送持久化消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIEW_COUNT_EXCHANGE,
                    RabbitMQConfig.VIEW_COUNT_ROUTING_KEY,
                    message,
                    msg -> {
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        msg.getMessageProperties().setMessageId(messageId);
                        return msg;
                    }
            );
            log.debug("阅读量消息发送成功：文章ID={}，messageId={}", articleId, messageId);


        } catch (Exception e) {
            log.error("发送阅读量消息失败，降级直接更新数据库", e);
            // 消息发送失败：降级更新数据库
            batchUpdateViewCount(articleId, 1);
        }
    }

    @Transactional
    @Override
    public void batchUpdateViewCount(Integer articleId, Integer increment) {
        if (articleId == null || increment == null || increment <= 0) {
            log.warn("无效参数：articleId={}，increment={}", articleId, increment);
            return;
        }

        // 批量更新数据库阅读量
        articleMapper.updateViewCountBatch(articleId, increment);
        // 更新后清理详情缓存（确保下次查询最新值）
        redisTemplate.delete(String.format(CACHE_KEY_DETAIL, articleId));
        log.debug("数据库阅读量更新成功：文章ID={}，增量={}", articleId, increment);
    }

    /**
     * 获取缓存的文章分页数据
     */
    private Page<Article> getArticleCache(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache instanceof Page) {
            Page<Article> result = (Page<Article>) cache;
            // 空结果不缓存（避免缓存无效数据）
            if (result.getTotal() == 0) {
                redisTemplate.delete(cacheKey);
                return null;
            }
            return result;
        }
        // 缓存类型错误：删除无效缓存
        if (cache != null) {
            redisTemplate.delete(cacheKey);
        }
        return null;
    }

    /**
     * 文章详情查询
     */
    @Override
    public Article getArticleDetail(Integer articleId) {
        if (articleId == null) {
            return null;
        }
        String hotCacheKey = String.format(HotArticleConstants.HOT_CACHE_KEY, articleId);
        Object hotCache = redisTemplate.opsForValue().get(hotCacheKey);
        Integer currentUserId = UserContextHolder.getUserId();

        if (hotCache instanceof Article) {
            if (currentUserId != null
                    && !currentUserId.equals(((Article) hotCache).getUserId())
                    && Objects.equals(((Article) hotCache).getStatus(), STATUS_PUBLISHED)) {
                hotArticleUtil.incrementViewCount(articleId);
            }
            return (Article) hotCache;
        }

        Article article = articleMapper.selectById(articleId);
        if (article != null) {
            if (currentUserId != null
                    && !currentUserId.equals(article.getUserId())
                    && Objects.equals(article.getStatus(), STATUS_PUBLISHED)) {
                hotArticleUtil.incrementViewCount(articleId);
            }

            article.setArticleId(articleId);
            hotArticleCacheManager.manageHotCache(article);
        }
        return article;
    }

    @Override
    public List<Article> getHotArticles() {
        Object hotArticles = redisTemplate.opsForValue().get(CACHE_KEY_HOT);
        if (hotArticles instanceof List) {
            log.debug("获取缓存成功：{}", CACHE_KEY_HOT);
            return (List<Article>) hotArticles;
        }
        if (hotArticles != null) {

            redisTemplate.delete(CACHE_KEY_HOT);
            log.warn("缓存类型错误：{}", CACHE_KEY_HOT);
        }
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getViewCount)
                .orderByAsc(Article::getUpdatedAt)
                .eq(Article::getStatus, 1)
                .last("LIMIT 8");
        List<Article> hotArticleList = articleMapper.selectList(queryWrapper);
        if (hotArticleList != null && !hotArticleList.isEmpty()) {
            redisTemplate.opsForValue().set(CACHE_KEY_HOT, hotArticleList, 30 + new Random().nextInt(5), TimeUnit.MINUTES);
            log.debug("热门文章写入缓存成功，缓存键: {}, 文章数量: {}", CACHE_KEY_HOT, hotArticleList.size());
        } else {
            log.debug("无热门文章数据，不写入缓存");
        }

        return hotArticleList;

    }

    @Override
    public long getArticleAllView() {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, STATUS_TO_BE_REVIEWED);
        return articleMapper.selectCount(queryWrapper);
    }

    @Override
    public Page<Article> searchArticles(Page<Article> page, String keyword, String sort) {
        return articleMapper.searchArticles(page, keyword, sort);
    }

    @Override
    public void deleteById(Integer articleId, Integer userId) {
        String cacheKey = "article:user:" + userId + "*";
        boolean b = removeById(articleId);
        if (b) {
            Set<String> keys = redisTemplate.keys(cacheKey);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    @Override
    public Page<Article> getArticle(Page<Article> articlePage) {
        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Article::getStatus, STATUS_TO_BE_REVIEWED);
        return this.page(articlePage, lambdaQueryWrapper);
    }
}