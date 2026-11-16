package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mpq.mapper.*;

import com.mpq.mapper.ArticleMapper;
import com.mpq.mapper.ArticleTagsMapper;
import com.mpq.mapper.TagsMapper;
import com.mpq.mapper.UserBehaviorLogMapper;
import com.mpq.pojo.*;
import com.mpq.service.OfflinePreferenceService;
import com.mpq.service.UserPreferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线用户偏好计算服务
 * 定时任务：每天凌晨3点执行
 * 任务流程：
 * 1. 批量获取近期有行为的用户列表
 * 2. 批量获取每个用户的行为数据
 * 3. 计算用户的分类偏好和标签偏好（含时间衰减）
 * 4. 将偏好结果写入 user_preferences 表
 */
@Slf4j
@Service
public class OfflinePreferenceServiceImpl implements OfflinePreferenceService {

    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final UserPreferenceService userPreferenceService;
    private final ArticleMapper articleMapper;
    private final ArticleTagsMapper articleTagMapper;
    private final TagsMapper tagsMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformTransactionManager transactionManager;

    // 统计天数：近30天
    private static final int STAT_DAYS = 30;
    // 分类偏好最大数量
    private static final int MAX_CATEGORY = 5;
    // 标签偏好最大数量
    private static final int MAX_TAG = 10;
    // 每批处理用户数
    private static final int USER_BATCH_SIZE = 200;
    // 本地缓存最大容量
    private static final int CACHE_MAX_SIZE = 10000;


    // 行为基础权值
    private static final double BASE_WEIGHT_VIEW = 2.0;     // 浏览
    private static final double BASE_WEIGHT_COLLECT = 5.0; // 收藏
    private static final double BASE_WEIGHT_LIKE = 3.0;      // 点赞
    // 时间半衰期（天）：7天前的行为权重衰减一半
    private static final double TIME_DECAY_HALF_LIFE = 7.0;
    // 偏好来源：1=离线计算
    private static final int SOURCE = 1;

    // 本地缓存：攻略文章分类（使用Caffeine LRU缓存）
    private final Cache<Integer, Integer> articleCategoryCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    // 本地缓存：攻略文章标签列表
    private final Cache<Integer, List<Integer>> articleTagIdsCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    // 本地缓存：标签名称
    private final Cache<Integer, String> tagNameCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    public OfflinePreferenceServiceImpl(UserBehaviorLogMapper userBehaviorLogMapper,
                                        UserPreferenceService userPreferenceService,
                                        ArticleMapper articleMapper,
                                        ArticleTagsMapper articleTagMapper,
                                        TagsMapper tagsMapper,
                                        RedisTemplate<String, Object> redisTemplate,
                                        PlatformTransactionManager transactionManager) {
        this.userBehaviorLogMapper = userBehaviorLogMapper;
        this.userPreferenceService = userPreferenceService;
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.tagsMapper = tagsMapper;
        this.redisTemplate = redisTemplate;
        this.transactionManager = transactionManager;
    }

    /**
     * 定时任务入口：每天凌晨3点执行
     */
    @Override
    @Scheduled(cron = "0 0 18 * * ?")
    public void computeUserPreference() {
        // 每天独立的进度key，避免跨天任务冲突
        String dateKey = LocalDate.now().toString();
        String progressKey = "preference:progress:" + dateKey;

        try {
            doCompute(progressKey);
        } finally {
            redisTemplate.delete(progressKey);
            log.info("===== 偏好计算任务结束 =====");
        }
    }

    /**
     * 按用户分批处理
     */
    public void doCompute(String progressKey) {
//        清空旧缓存
        articleCategoryCache.invalidateAll();
        articleTagIdsCache.invalidateAll();
        tagNameCache.invalidateAll();

        LocalDateTime now = LocalDateTime.now();
        // 统计近30天的行为
        LocalDateTime startTime = now.minusDays(STAT_DAYS);

        // 获取上次进度
        Object progressObj = redisTemplate.opsForValue().get(progressKey);
        int lastUserId =    progressObj != null ? Integer.parseInt(progressObj.toString()) : 0;
        int totalProcessed = 0;
        int failedUsers = 0;

        while (true) {
            // 1. 批量获取本批次所有用户ID及行为
            Map<Integer, List<UserBehaviorLog>> batchUserActions = fetchBatchUserActions(startTime, lastUserId, USER_BATCH_SIZE);
            if (batchUserActions.isEmpty()) {
                break;
            }

            // 2. 收集本次所有攻略文章ID，批量预加载到本地缓存
            Set<Integer> articleIds = batchUserActions.values()
                    .stream()
                    .flatMap(List::stream)
                    .map(UserBehaviorLog::getTargetId)
                    .collect(Collectors.toSet());
            preloadCache(articleIds);

            // 3. 逐用户计算偏好
            List<Integer> userIds = new ArrayList<>(batchUserActions.keySet());
            Collections.sort(userIds);
            for (Integer userId : userIds) {
                try {
                    List<UserBehaviorLog> userActions = batchUserActions.get(userId);
                    if (userActions == null || userActions.isEmpty()) {
                        continue;
                    }
                    // 使用独立事务处理每个用户
                    processUserWithTransaction(userId, userActions, now);
                    totalProcessed++;
                } catch (Exception e) {
                    failedUsers++;
                    log.error("用户{}处理失败: {}", userId, e.getMessage());
                }
            }

            // 4. 更新进度
            lastUserId = userIds.get(userIds.size() - 1);
            redisTemplate.opsForValue().set(progressKey, String.valueOf(lastUserId));

            log.info("已处理{}个用户，当前进度userId={}", totalProcessed, lastUserId);

            // 如果读取数量小于批次大小，说明已经是最后一批
            if (batchUserActions.size() < USER_BATCH_SIZE) {
                break;
            }
        }

        log.info("离线用户偏好计算完成，处理用户数：{}，失败用户数：{}", totalProcessed, failedUsers);
    }

    /**
     * 批量获取用户ID及行为
     */
    private Map<Integer, List<UserBehaviorLog>> fetchBatchUserActions(LocalDateTime startTime, int lastUserId, int limit) {
        // 1. 分页获取有行为的用户ID列表
        List<Integer> userIds = fetchUserIds(startTime, lastUserId, limit);
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 一次查询获取这些用户的所有行为
        LambdaQueryWrapper<UserBehaviorLog> query = new LambdaQueryWrapper<>();
        query.in(UserBehaviorLog::getUserId, userIds)
                .in(UserBehaviorLog::getActionType, Arrays.asList(2, 3, 4))  // 浏览、收藏、点赞
                .eq(UserBehaviorLog::getTargetType, 1)  // 攻略文章
                .ge(UserBehaviorLog::getCreatedAt, startTime)
                .select(UserBehaviorLog::getLogId, UserBehaviorLog::getUserId,
                        UserBehaviorLog::getTargetId, UserBehaviorLog::getActionType,
                        UserBehaviorLog::getCreatedAt);

        List<UserBehaviorLog> allActions = userBehaviorLogMapper.selectList(query);

        // 3. 按用户ID分组
        return allActions.stream()
                .collect(Collectors.groupingBy(UserBehaviorLog::getUserId));
    }

    /**
     * 获取用户ID列表
     */
    private List<Integer> fetchUserIds(LocalDateTime startTime, int lastUserId, int limit) {
        LambdaQueryWrapper<UserBehaviorLog> query = new LambdaQueryWrapper<>();
        query.in(UserBehaviorLog::getActionType, Arrays.asList(2, 3, 4))
                .eq(UserBehaviorLog::getTargetType, 1)
                .ge(UserBehaviorLog::getCreatedAt, startTime)
                .gt(lastUserId > 0, UserBehaviorLog::getUserId, lastUserId)
                .select(UserBehaviorLog::getUserId)
                .groupBy(UserBehaviorLog::getUserId)
                .orderByAsc(UserBehaviorLog::getUserId)
                .last("LIMIT " + limit);

        return userBehaviorLogMapper.selectList(query).stream()
                .map(UserBehaviorLog::getUserId)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 批量预加载文章分类和标签到本地缓存
     */
    private void preloadCache(Set<Integer> articleIds) {
        if (articleIds.isEmpty()) return;

        // 批量查询攻略文章分类
        LambdaQueryWrapper<Article> articleQuery = new LambdaQueryWrapper<>();
        articleQuery.in(Article::getArticleId, articleIds)
                .select(Article::getArticleId, Article::getCategoryId);
        List<Article> articles = articleMapper.selectList(articleQuery);
        for (Article article : articles) {
            if (article.getCategoryId() != null) {
                articleCategoryCache.put(article.getArticleId(), article.getCategoryId());
            }
        }

        // 批量查询文章标签
        LambdaQueryWrapper<ArticleTags> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.in(ArticleTags::getArticleId, articleIds);
        List<ArticleTags> articleTags = articleTagMapper.selectList(tagQuery);

        // 按文章ID分组
        Map<Integer, List<Integer>> tagIdMap = articleTags.stream()
                .collect(Collectors.groupingBy(
                        ArticleTags::getArticleId,
                        Collectors.mapping(ArticleTags::getTagId, Collectors.toList())));
        for (Map.Entry<Integer, List<Integer>> entry : tagIdMap.entrySet()) {
            articleTagIdsCache.put(entry.getKey(), entry.getValue());
        }

        // 批量查询标签名称
        Set<Integer> tagIds = articleTags.stream()
                .map(ArticleTags::getTagId)
                .collect(Collectors.toSet());
        if (!tagIds.isEmpty()) {
            LambdaQueryWrapper<Tags> nameQuery = new LambdaQueryWrapper<>();
            nameQuery.in(Tags::getTagId, tagIds)
                    .select(Tags::getTagId, Tags::getTagName);
            List<Tags> tags = tagsMapper.selectList(nameQuery);
            for (Tags tag : tags) {
                tagNameCache.put(tag.getTagId(), tag.getTagName());
            }
        }
    }

    /**
     * 使用事务处理单个用户
     */
    private void processUserWithTransaction(Integer userId, List<UserBehaviorLog> userActions, LocalDateTime now) {

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        tx.execute(status -> {
            processUserPreference(userId, userActions, now);
            return null;
        });
    }

    /**
     * 处理单个用户偏好：计算并保存
     */
    public void processUserPreference(Integer userId, List<UserBehaviorLog> userActions, LocalDateTime now) {
        if (userActions == null || userActions.isEmpty()) {
            return;
        }

        // 1. 计算每篇文章的加权权重
        Map<Integer, Double> articleWeightMap = calculateArticleWeights(userActions, now);

        // 2. 汇总分类和标签权重
        Map<Integer, Double> categoryTotalWeight = new HashMap<>();
        Map<Integer, Double> tagTotalWeight = new HashMap<>();

        for (Map.Entry<Integer, Double> entry : articleWeightMap.entrySet()) {
            Integer articleId = entry.getKey();
            Double weight = entry.getValue();
            if (weight <= 0) continue;

            // 从缓存获取文章分类
            Integer categoryId = articleCategoryCache.getIfPresent(articleId);
            if (categoryId != null) {
                categoryTotalWeight.merge(categoryId, weight, Double::sum);
            }

            // 从缓存获取文章标签
            List<Integer> tagIds = articleTagIdsCache.getIfPresent(articleId);
            if (tagIds != null) {
                Set<Integer> uniqueTagIds = new HashSet<>(tagIds);
                for (Integer tagId : uniqueTagIds) {
                    tagTotalWeight.merge(tagId, weight, Double::sum);
                }
            }
        }

        // 3. 计算最大值
        double maxCategoryWeight = categoryTotalWeight.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);
        double maxTagWeight = tagTotalWeight.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);

        if (maxCategoryWeight <= 0 && maxTagWeight <= 0) {
            return;
        }

        // 4. 生成偏好记录
        List<UserPreferences> newPreferences = new ArrayList<>();

        // 分类偏好
        if (maxCategoryWeight > 0) {

            for (Map.Entry<Integer, Double> entry : categoryTotalWeight.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(MAX_CATEGORY)
                    .collect(Collectors.toList())) {
                UserPreferences pref = new UserPreferences();
                pref.setUserId(userId);
                pref.setPreferType(2);  // 分类偏好
                pref.setCategoryId(entry.getKey());
                pref.setPreferValue(entry.getKey().toString());
                pref.setPreferWeight(normalize(entry.getValue(), maxCategoryWeight));
                BigDecimal confidence = calculateConfidence(entry.getValue(), maxCategoryWeight);
                if (confidence.compareTo(BigDecimal.valueOf(0.1)) <=0){
                    continue;
                }
                pref.setConfidence(confidence);
                pref.setSource(SOURCE);
                pref.setUpdateTime(now);
                newPreferences.add(pref);
            }
        }

        // 标签偏好
        if (maxTagWeight > 0) {
            for (Map.Entry<Integer, Double> entry : tagTotalWeight.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(MAX_TAG)
                    .collect(Collectors.toList())) {
                String tagName = tagNameCache.getIfPresent(entry.getKey());
                UserPreferences pref = new UserPreferences();
                pref.setUserId(userId);
                pref.setPreferType(1);  // 标签偏好
                pref.setTagId(entry.getKey());
                pref.setPreferValue(tagName != null ? tagName : "");
                pref.setPreferWeight(normalize(entry.getValue(), maxTagWeight));
                BigDecimal confidence = calculateConfidence(entry.getValue(), maxTagWeight);
                if (confidence.compareTo(BigDecimal.valueOf(0.1)) <=0){
                    continue;
                }
                pref.setConfidence(confidence);
                pref.setSource(SOURCE);
                pref.setUpdateTime(now);

                newPreferences.add(pref);
            }
        }

        // 5. 先删旧偏好，再插入新偏好
        if (!newPreferences.isEmpty()) {
            userPreferenceService.deleteByUserId(userId);
            userPreferenceService.saveBatch(newPreferences);
        }
    }

    /**
     * 计算每篇文章的加权权重
     * 公式：基础权值 × 时间衰减系数
     */
    private Map<Integer, Double> calculateArticleWeights(List<UserBehaviorLog> userActions, LocalDateTime now) {
        Map<Integer, Double> result = new HashMap<>();
        for (UserBehaviorLog action : userActions) {
            // 获取基础权值
            double baseWeight = switch (action.getActionType()) {
                case 2 -> BASE_WEIGHT_VIEW;      // 浏览
                case 3 -> BASE_WEIGHT_COLLECT;   // 收藏
                case 4 -> BASE_WEIGHT_LIKE;    // 点赞
                default -> 0.0;
            };
            if (baseWeight <= 0) continue;

            // 计算时间衰减系数（7天）
            double timeWeight = calculateTimeWeight(action.getCreatedAt(), now);
            double totalWeight = baseWeight * timeWeight;

            // 累加同一篇文章的权重
            result.merge(action.getTargetId(), totalWeight, Double::sum);
        }
        return result;
    }

    /**
     * 计算时间衰减系数
     * 使用半衰期公式：0.5^(天数/半衰期)
     */
    private double calculateTimeWeight(LocalDateTime actionTime, LocalDateTime now) {
        long days = ChronoUnit.DAYS.between(actionTime, now);
        if (days <= 0) return 1.0;
        return Math.pow(0.5, (double) days / TIME_DECAY_HALF_LIFE);
    }

    /**
     * 权重归一化：基于用户自身最大值归一化到0-10分
     */
    private BigDecimal normalize(double value, double maxWeight) {
        if (maxWeight <= 0) return BigDecimal.ZERO;
        double normalized = (value / maxWeight) * 10.0;
        normalized = Math.max(0, Math.min(10, normalized));
        return BigDecimal.valueOf(normalized).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 置信度计算：基于权重占比
     */
    private BigDecimal calculateConfidence(double value, double maxWeight) {
        if (maxWeight <= 0) return BigDecimal.ZERO;
        double confidence = value / maxWeight;
        confidence = Math.max(0, Math.min(1, confidence));
        return BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP);
    }
}