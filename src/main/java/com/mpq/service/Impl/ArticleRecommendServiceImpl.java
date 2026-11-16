package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.mapper.ArticleMapper;
import com.mpq.mapper.ArticleTagsMapper;
import com.mpq.mapper.TagsMapper;
import com.mpq.mapper.UserPreferenceMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.ArticleTags;
import com.mpq.pojo.Tags;
import com.mpq.pojo.UserPreferences;
import com.mpq.service.ArticleRecommendService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRecommendServiceImpl implements ArticleRecommendService {
    private final UserPreferenceMapper userPreferenceMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagsMapper articleTagMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TagsMapper tagsMapper;

    private static final int STATUS_PUBLISH = 1;

    private static final int PREFER_TYPE_TAG = 1;
    private static final int PREFER_TYPE_CATEGORY = 2;

    // 修复：缓存Key包含pageSize，避免不同每页条数缓存冲突
    private static final String REC_CACHE_KEY = "rec:user:%d:page:%d:size:%d";
    private static final String HOT_CACHE_KEY_LIST = "rec:hot:page:%d:%d";
    private static final Integer HOT_CACHE_TTL = 20; // 热门缓存20分钟
    private static final int REC_CACHE_TTL = 15; // 推荐缓存15分钟
    private static final int RECALL_LIMIT = 500; // 扩大召回数量至500篇
    private static final int DEFAULT_RECALL_DAYS = 30; // 默认召回30天内文章
    private static final int CANDIDATE_POOL_SIZE = 200; // 候选池固定200篇


    @Override
    public Page<Article> getPersonalRecommend(int pageNum, int pageSize) {

        Integer userId = UserContextHolder.getUserId();
        if (userId == null) {
            log.warn("用户未登录，返回热门文章分页");
            return getHotArticlePage(pageNum, pageSize);
        }

        // 候选池缓存Key
        String poolCacheKey = String.format("rec:user:%d:pool:%d:size:%d", userId,
                CANDIDATE_POOL_SIZE, pageSize);
        List<Article> candidatePool = (List<Article>) redisTemplate.opsForValue().get(poolCacheKey);

        if (candidatePool == null) {
            // 缓存未命中，生成200篇候选文章,个性化+兜底
            candidatePool = generateCandidatePool(userId);
            redisTemplate.opsForValue().set(poolCacheKey, candidatePool, REC_CACHE_TTL, TimeUnit.MINUTES);
            log.debug("用户{}候选池缓存未命中，生成{}篇候选文章并缓存", userId, candidatePool.size());
        }

        // 基于前端传入的pageNum和pageSize，正确截取候选池数据
        Page<Article> resultPage = buildPagedResult(candidatePool, pageNum, pageSize);

        return resultPage;
    }

    //    生成用户个性化候选文章池，
    private List<Article> generateCandidatePool(Integer userId) {
        List<Article> finalArticles = new ArrayList<>();
        // 1. 获取用户偏好,分类和标签
        List<UserPreferences> userPrefers = getUserPreferences(userId);
        if (!userPrefers.isEmpty()) {

            // 提取偏好中的分类ID和标签名
            List<Integer> preferCategories = userPrefers.stream()
                    .filter(p -> p.getPreferType().equals(PREFER_TYPE_CATEGORY))
                    .map(p -> Integer.parseInt(p.getPreferValue()))
                    .toList();

            List<String> preferTags = userPrefers.stream()
                    .filter(p -> p.getPreferType().equals(PREFER_TYPE_TAG))
                    .map(UserPreferences::getPreferValue)
                    .toList();
            // 召回候选文章,最多500篇，近30天
            List<Article> candidates = recallArticles(preferCategories, preferTags, DEFAULT_RECALL_DAYS);
            if (!candidates.isEmpty()) {
                // 按偏好匹配度、时效性、质量分排序
                finalArticles = sortArticles(candidates, preferCategories, preferTags, userPrefers);
            }
        }
        // 2. 若数量不足目标池大小，逐级补足
        int need = CANDIDATE_POOL_SIZE - finalArticles.size();
        if (need > 0) {
            // 优先补混合推荐（近期浏览分类 + 热门）
            List<Article> mixed = getMixedRecommendFora(userId, need);
            finalArticles.addAll(mixed);
            need = CANDIDATE_POOL_SIZE - finalArticles.size();

            if (need > 0) {
                // 其次补近30天热门文章
                List<Article> hot = getHotArticlesFora(need, 30, userId);
                finalArticles.addAll(hot);
                need = CANDIDATE_POOL_SIZE - finalArticles.size();
            }

            if (need > 0) {
                // 最后补最新文章
                List<Article> latest = getLatestArticlesFora(need, userId);
                finalArticles.addAll(latest);
            }
        }
        // 3. 按文章ID去重，并截取前200篇返回
        return finalArticles.stream()
                .collect(Collectors.toMap(Article::getArticleId, a -> a, (a1, a2) -> a1))
                .values()
                .stream()
                .limit(CANDIDATE_POOL_SIZE)
                .collect(Collectors.toList());
    }

    /**
     * 混合推荐补足,用于数量不足
     */
    private List<Article> getMixedRecommendFora(Integer userId, int need) {
        List<Article> mixed = new ArrayList<>();

        // 1. 近期浏览分类文章,最多50%
        int browseNeed = Math.min(need / 2, need);
        List<Article> browsed = getBrowsedCategoryArticles(userId, browseNeed);
        mixed.addAll(browsed);
        need -= browsed.size();
        if (need <= 0) {
            // 截取实际需要的数量
            return mixed.stream().limit(browseNeed).collect(Collectors.toList());
        }

        // 2. 热门文章补足剩余
        List<Article> hot = getHotArticlesForMixed(need, 7, userId);
        mixed.addAll(hot);
        // 返回不超过原始需求的数量
        return mixed.stream().limit(browseNeed + need).collect(Collectors.toList());
    }


    /**
     * 热门文章分页
     */
    private Page<Article> getHotArticlePage(int pageNum, int pageSize) {
        String cacheKey = String.format(HOT_CACHE_KEY_LIST, pageNum, pageSize);
        Page<Article> cachePage = (Page<Article>) redisTemplate.opsForValue().get(cacheKey);
        if (cachePage != null) {
            log.debug("从缓存获取热门文章分页，key={}，页码={}，每页条数={}", cacheKey, pageNum, pageSize);
            return cachePage;
        }

        // 查询近30天热门文章,查询10倍于pageSize的数量
        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        query.eq(Article::getStatus, 1)
                .ge(Article::getUpdatedAt, LocalDateTime.now().minusDays(30))
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .last("LIMIT " + (pageSize * 10));

        List<Article> hotArticles = articleMapper.selectList(query);
        Page<Article> resultPage = buildPagedResult(hotArticles, pageNum, pageSize);

        // 缓存热门文章分页
        redisTemplate.opsForValue().set(cacheKey, resultPage, HOT_CACHE_TTL, TimeUnit.MINUTES);
        log.debug("热门文章分页写入缓存，key={}，总条数={}，总页数={}", cacheKey, resultPage.getTotal(), resultPage.getPages());
        return resultPage;
    }


    /**
     * 构建分页结果，页码计算和数据截取逻辑
     */
    private Page<Article> buildPagedResult(List<Article> fullPool, int pageNum, int pageSize) {
        // 页码最小为1，避免负数或0页
        pageNum = Math.max(pageNum, 1);
        // pageSize最小为10，最大为50，避免异常
        pageSize = Math.max(Math.min(pageSize, 50), 10);

        int total = fullPool.size();
        // 总页数计算,向上取整
        int totalPages = (total + pageSize - 1) / pageSize;

        // 计算数据截取范围,避免start >= end导致空数据
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<Article> currentPageRecords = new ArrayList<>();
        if (start < end && !fullPool.isEmpty()) {
            // 使用subList后转为新ArrayList，避免ConcurrentModificationException
            currentPageRecords = new ArrayList<>(fullPool.subList(start, end));
        }

        // 构建MyBatis-Plus Page对象，返回完整分页信息（总条数、总页数、当前页数据）
        Page<Article> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.setPages(totalPages);
        page.setRecords(currentPageRecords);



        log.debug("分页结果：页码={}，每页条数={}，总条数={}，总页数={}，当前页数据条数={}",
                pageNum, pageSize, total, totalPages, currentPageRecords.size());
        return page;
    }


    /**
     * 个性化排序按偏好匹配度、时效性、质量加权
     */
    private List<Article> sortArticles(List<Article> candidates,
                                       List<Integer> preferCategories,
                                       List<String> preferTags,
                                       List<UserPreferences> userPrefers) {
        Map<Integer, List<String>> articleTagsMap = loadArticleTags(candidates);
        Map<String, Double> preferWeightMap = buildPreferWeightMap(userPrefers);

        return candidates.stream()
                .map(article -> {
                    double totalScore = 0.0;
                    Integer category = article.getCategoryId();
                    List<String> tags = articleTagsMap.getOrDefault(article.getArticleId(), Collections.emptyList());

                    // 1. 偏好匹配得分（40%）
                    double matchScore = calculateMatchScore(category, tags, preferWeightMap);
                    totalScore += matchScore * 0.4;

                    // 2. 时效性得分（30%）
                    double timelyScore = calculateTimelyScore(article.getUpdatedAt());
                    totalScore += timelyScore * 0.3;

                    // 3. 质量得分（30%）
                    double qualityScore = calculateQualityScore(article);
                    totalScore += qualityScore * 0.3;

                    article.setSortScore(totalScore);
                    return article;
                })
                .sorted((a1, a2) -> Double.compare(a2.getSortScore(), a1.getSortScore())) // 降序排序
                .collect(Collectors.toList());
    }


    /**
     * 候选文章召回
     */
    private List<Article> recallArticles(List<Integer> preferCategories, List<String> preferTags, int recallDays) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(recallDays);
        Integer userId = UserContextHolder.getUserId();
        // 查询
        return articleMapper.recallArticles(
                STATUS_PUBLISH,  // status=1（已发布）
                startTime,
                preferCategories,
                preferTags,
                RECALL_LIMIT  // 召回数量限制
                , userId
        );

    }

    /**
     * 获取用户偏好
     */
    private List<UserPreferences> getUserPreferences(Integer userId) {
        LambdaQueryWrapper<UserPreferences> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPreferences::getUserId, userId)
                .orderByDesc(UserPreferences::getPreferWeight); // 高权重优先
        return userPreferenceMapper.selectList(queryWrapper);
    }

    /**
     * 预加载文章标签
     */
    private Map<Integer, List<String>> loadArticleTags(List<Article> candidates) {
        //提取攻略id
        List<Integer> articleIds = candidates.stream()
                .map(Article::getArticleId)
                .toList();
        if (articleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查 article_tags
        LambdaQueryWrapper<ArticleTags> query = new LambdaQueryWrapper<>();
        query.in(ArticleTags::getArticleId, articleIds);
        List<ArticleTags> articleTagsList = articleTagMapper.selectList(query);
        if (articleTagsList.isEmpty()) {
            return Collections.emptyMap();
        }

        // 获取所有 tagId，查 tags 表
        List<Integer> tagIds = articleTagsList.stream()
                .map(ArticleTags::getTagId)
                .distinct()
                .toList();
        LambdaQueryWrapper<Tags> tagsQuery = new LambdaQueryWrapper<>();
        tagsQuery.in(Tags::getTagId, tagIds)
                .select(Tags::getTagId, Tags::getTagName);
        List<Tags> tagsList = tagsMapper.selectList(tagsQuery);

        // 构建 tagId -> tagName 映射
        //去重
        Map<Integer, String> tagIdNameMap = tagsList.stream()
                .collect(Collectors.toMap(Tags::getTagId, Tags::getTagName, (a, b) -> a));
        // 构建 articleId -> tagNames 映射
        Map<Integer, List<String>> tagMap = new HashMap<>();
        for (ArticleTags at : articleTagsList) {
            String tagName = tagIdNameMap.get(at.getTagId());
            if (tagName != null) {
                tagMap.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>()).add(tagName);
            }
        }
        return tagMap;
    }

    /**
     * 构建偏好权重映射
     */
    private Map<String, Double> buildPreferWeightMap(List<UserPreferences> userPrefers) {
        Map<String, Double> weightMap = new HashMap<>();
        for (UserPreferences pref : userPrefers) {
            String key = pref.getPreferType().equals(PREFER_TYPE_CATEGORY)
                    ? "C_" + pref.getPreferValue()
                    : "T_" + pref.getPreferValue();
            // BigDecimal 转 Double
            double weight = pref.getPreferWeight() != null ? pref.getPreferWeight().doubleValue() : 0.0;
            weightMap.put(key, weight);
        }
        return weightMap;
    }

    /**
     * 计算偏好匹配得分（0-10分）
     */
    private double calculateMatchScore(Integer category, List<String> tags, Map<String, Double> weightMap) {
        double total = 0.0;
        // 分类匹配
        if (category != null) {
            total += weightMap.getOrDefault("C_" + category, 0.0);
        }
        // 标签匹配
        for (String tag : tags) {
            total += weightMap.getOrDefault("T_" + tag, 0.0);
        }
        return Math.min(10.0, total); // 上限10分
    }

    /**
     * 计算时效性得分（0-10分）
     */
    private double calculateTimelyScore(LocalDateTime updatedAt) {
        long days = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());
        return switch ((int) days) {
            case 0 -> 10.0;
            case 1 -> 9.0;
            case 2 -> 8.0;
            case 3 -> 7.0;
            case 4 -> 6.0;
            case 5 -> 5.0;
            case 6 -> 4.0;
            default -> Math.max(1.0, 4.0 - (days - 7) / 7.0); // 超过1周缓慢降分
        };
    }

    /**
     * 计算质量得分（0-10分）
     */
    private double calculateQualityScore(Article article) {
        int views = article.getViewCount() == null ? 0 : article.getViewCount();
        int likes = article.getLikeCount() == null ? 0 : article.getLikeCount();
        int comments = article.getCommentCount() == null ? 0 : article.getCommentCount();
        double score = 0.0;
        // 阅读量（0-3分）
        score += views >= 1000 ? 3.0 : views / 1000.0 * 3;
        // 点赞率（0-3分）
        if (views > 0) {
            double likeRate = (double) likes / views;
            score += likeRate > 0.1 ? 3.0 : (likeRate > 0.04 ? 1.0 : 0.0);
        }
        // 评论率（0-4分）
        if (views > 0) {
            double commentRate = (double) comments / views;
            score += commentRate > 0.05 ? 4.0 : (commentRate > 0.02 ? 1.0 : 0);
        }

        return Math.min(10.0, score);
    }

    /**
     * 获取用户近期浏览分类的文章
     */
    private List<Article> getBrowsedCategoryArticles(Integer userId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        // 查询用户近7天浏览过的分类
        List<Integer> browsedCategories = articleMapper.selectBrowsedCategories(userId, 7);
        if (browsedCategories.isEmpty()) {
            log.debug("用户{}无近期浏览分类，跳过该部分召回", userId);
            return Collections.emptyList();
        }

        // 召回这些分类下的近30天文章
        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        query.eq(Article::getStatus, 1)
                .in(Article::getCategoryId, browsedCategories)
                .ge(Article::getUpdatedAt, LocalDateTime.now().minusDays(30))
                .orderByDesc(Article::getUpdatedAt)
                .ne(Article::getUserId, userId)
                .last("LIMIT " + limit);

        return articleMapper.selectList(query);
    }

    /**
     * 按时间范围获取热门文章
     */
    private List<Article> getHotArticlesForMixed(int limit, int days, Integer userId) {
        if (limit <= 0) return Collections.emptyList();

        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        query.eq(Article::getStatus, 1)
                .ge(Article::getUpdatedAt, LocalDateTime.now().minusDays(days))
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .last("LIMIT " + limit);
        if (userId != null) {
            query.ne(Article::getUserId, userId);
        }

        return articleMapper.selectList(query);
    }

//    /**
//     * 默认查询近7天热门文章
//     */
//    private List<Article> getHotArticlesForMixed(int limit) {
//        return getHotArticlesForMixed(limit, 7, null);
//    }

    /**
     * 补足用的热门文章
     */
    private List<Article> getHotArticlesFora(int need, int days, Integer userId) {
        if (need <= 0) return Collections.emptyList();
        days = Math.max(days, 7); // 至少查7天

        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        query.eq(Article::getStatus, 1)
                .ge(Article::getUpdatedAt, LocalDateTime.now().minusDays(days))
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getLikeCount)
                .ne(Article::getUserId, userId)
                .last("LIMIT " + need);

        List<Article> hot = articleMapper.selectList(query);
        log.debug("热门文章补足：近{}天获取{}篇（需{}篇）", days, hot.size(), need);
        return hot;
    }

    /**
     * 获取最新发布的文章（兜底用）
     */
    private List<Article> getLatestArticles(int limit, Integer userId) {
        if (limit <= 0) return Collections.emptyList();

        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        query.eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreatedAt)
                .ne(Article::getUserId, userId)
                .last("LIMIT " + limit);

        return articleMapper.selectList(query);
    }


    /**
     * 补足用的最新文章
     */
    private List<Article> getLatestArticlesFora(int need, Integer userId) {
        List<Article> latest = getLatestArticles(need, userId);
        log.debug("最新文章兜底：获取{}篇（需{}篇）", latest.size(), need);
        return latest;
    }


}