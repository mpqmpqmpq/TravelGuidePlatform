package com.mpq.constants;

import org.springframework.stereotype.Component;

@Component
public class HotArticleConstants {
    // 1. Redis指标存储Key（阅读量、点赞量、发布时间）
    public static final String VIEW_COUNT_KEY = "article:metric:view:%d";       // 阅读量计数器
    public static final String LIKE_COUNT_KEY = "article:metric:like:%d";       // 点赞量计数器
    public static final String PUBLISH_TIME_KEY = "article:metric:publish:%d"; // 发布时间（时间戳）

    // 2. 热点缓存Key
    public static final String HOT_CACHE_KEY = "article:hot:detail:%d";        // 热点文章详情缓存
    public static final String HOT_ID_SET_KEY = "article:hot:ids";             // 所有热点ID集合（控制总量）
    //缓存媒体key
    public static final String MEDIA_CACHE_KEY = "article:id:%d:media:detail:%d";

    // 3. 动态配置
    public static final String HOT_THRESHOLD_KEY = "article:hot:threshold";    // 热度分阈值（默认60）
    public static final String MAX_HOT_SIZE_KEY = "article:hot:max_size";      // 最大热点数量（默认500）

    // 4. 默认值
    public static final int DEFAULT_HOT_THRESHOLD = 60;   // 默认热度阈值
    public static final int DEFAULT_MAX_HOT_SIZE = 500;   // 默认最大热点数量
    public static final int HOT_CACHE_TTL = 30;           // 热点缓存过期时间（分钟）
}