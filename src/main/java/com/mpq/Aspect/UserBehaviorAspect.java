package com.mpq.Aspect;

import com.mpq.config.RabbitMQConfig;
import com.mpq.pojo.Article;
import com.mpq.pojo.UserBehaviorLog;

import com.mpq.pojo.RecommendationEffects;
import com.mpq.service.RecommendationEffectsService;
import com.mpq.utils.UserContextHolder;
import com.mpq.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class UserBehaviorAspect {


    private final RecommendationEffectsService recommendationEffectsService;

    private final RedisTemplate<String,Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    // 目标类型常量
    private static final Integer TARGET_TYPE_ARTICLE = 1;

    private static final Integer ACTION_CLICK = 2;      // 点击（浏览详情）
    private static final Integer ACTION_COLLECT = 3;    // 收藏
    private static final Integer ACTION_LIKE = 4;       // 点赞

    public UserBehaviorAspect(
                              RecommendationEffectsService recommendationEffectsService,
                            RedisTemplate<String, Object> redisTemplate, RabbitTemplate rabbitTemplate) {

        this.recommendationEffectsService = recommendationEffectsService;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 切入点：拦截文章详情浏览（点击行为）
     */
    @Pointcut("execution(com.mpq.pojo.Article com.mpq.service.ArticleService.getArticleDetail(Integer)) && args(articleId)")
    public void articleDetailPointcut(Integer articleId) {
    }

    /**
     * 切入点：拦截文章点赞行为
     */
    @Pointcut("execution(boolean com.mpq.service.LikeService.likeArticle(Integer, Integer)) && args(articleId, userId)")
    public void likeArticlePointcut(Integer articleId, Integer userId) {
    }

    /**
     * 切入点：拦截文章收藏行为
     */
    @Pointcut("execution(com.mpq.common.Result com.mpq.service.BookMarksService.addBookMarks(Integer, Integer)) && args(articleId, userId)")
    public void bookMarksArticlePointcut(Integer articleId, Integer userId) {
    }

    // 浏览（点击）
    @AfterReturning(value = "articleDetailPointcut(articleId)", returning = "result")
    public void recordArticleView( Integer articleId, Object result) {
        Integer userId = UserContextHolder.getUserId();
        String sessionId = UserContextHolder.getSessionId();// 获取额外信息（如来源、设备等）
        String extra = UserContextHolder.getExtra(); // 获取行为值（如点击位置、推荐来源等）
        Integer actionValue = UserContextHolder.getActionValue(); // 获取行为值（如点击位置、推荐来源等）
        if (userId == null || !(result instanceof Article)) {
            log.debug("跳过浏览记录：userId为空或返回非文章对象");
            return;
        }
        String key = "behavior:recent:"+userId+":"+articleId+":"+ACTION_CLICK;


        if (redisTemplate.hasKey(key)){
            return;
        }
        redisTemplate.opsForValue().set(key,"1",5, TimeUnit.MINUTES);

        // 获取曝光记录中的 recall_source 和 rec_position
        RecommendationEffects exposure = recommendationEffectsService.getLatestExposure(userId, articleId);
        Integer recallSource = exposure != null ? exposure.getRecallSource() : null;
        Integer recPosition = exposure != null ? exposure.getRecPosition() : null;

        UserBehaviorLog logEntry = new UserBehaviorLog();
        logEntry.setUserId(userId);
        logEntry.setSessionId(sessionId);
        logEntry.setTargetType(TARGET_TYPE_ARTICLE);
        logEntry.setTargetId(articleId);
        logEntry.setActionType(ACTION_CLICK);
        logEntry.setActionValue(actionValue);
        logEntry.setRecallSource(recallSource);
        logEntry.setRecPosition(recPosition);
        logEntry.setExtra(extra);
        logEntry.setCreatedAt(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BEHAVIOR_LOG_EXCHANGE,
                RabbitMQConfig.BEHAVIOR_LOG_ROUTING_KEY,
                logEntry

        );



    }

    // 点赞
    @AfterReturning(value = "likeArticlePointcut(articleId, userId)", returning = "isSuccess")
    public void recordArticleLike(Integer articleId, Integer userId, boolean isSuccess) {
        if (!isSuccess || userId == null) {
            log.debug("跳过点赞记录：操作失败或userId为空");
            return;
        }

        String sessionId = UserContextHolder.getSessionId();
        String extra = UserContextHolder.getExtra();
        Integer actionValue = UserContextHolder.getActionValue();

        UserBehaviorLog logEntry = new UserBehaviorLog();
        logEntry.setUserId(userId);
        logEntry.setSessionId(sessionId);
        logEntry.setTargetType(TARGET_TYPE_ARTICLE);
        logEntry.setTargetId(articleId);
        logEntry.setActionType(ACTION_LIKE);
        logEntry.setActionValue(actionValue);
        logEntry.setExtra(extra);
        logEntry.setCreatedAt(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BEHAVIOR_LOG_EXCHANGE,
                RabbitMQConfig.BEHAVIOR_LOG_ROUTING_KEY,
                logEntry

        );
    }

    //收藏
    @AfterReturning(value = "bookMarksArticlePointcut(articleId, userId)", returning = "result")
    public void recordArticleBookMarks( Integer articleId, Integer userId, Result result) {
        if (result == null || result.getCode() != 200 || userId == null) {
            log.debug("跳过收藏记录：操作失败或userId为空");
            return;
        }

        String sessionId = UserContextHolder.getSessionId();
        String extra = UserContextHolder.getExtra();
        Integer actionValue = UserContextHolder.getActionValue();

        UserBehaviorLog logEntry = new UserBehaviorLog();
        logEntry.setUserId(userId);
        logEntry.setSessionId(sessionId);
        logEntry.setTargetType(TARGET_TYPE_ARTICLE);
        logEntry.setTargetId(articleId);
        logEntry.setActionType(ACTION_COLLECT);
        logEntry.setActionValue(actionValue);
        logEntry.setExtra(extra);
        logEntry.setCreatedAt(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BEHAVIOR_LOG_EXCHANGE,
                RabbitMQConfig.BEHAVIOR_LOG_ROUTING_KEY,
                logEntry

        );
    }
}