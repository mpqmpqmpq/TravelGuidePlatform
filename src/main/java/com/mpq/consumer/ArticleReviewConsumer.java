package com.mpq.consumer;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.mpq.config.RabbitMQConfig;
import com.mpq.mapper.ArticleReviewMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.ArticleReview;
import com.mpq.utils.IntelligentAuditingUtil;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleReviewConsumer {

    private final ArticleReviewMapper articleReviewMapper;

    @Value("${application.id.content}")
    private String APPLICATION_ID;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.REVIEW_QUEUE, ackMode = "MANUAL")
    public void handleReviewResult(Map<String, Object> reviewTask, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到消息");
            // 解析消息，获取攻略ID和内容
            Integer articleId = (Integer) reviewTask.get("articleId");
            log.info("文章ID：{}", articleId);
            String content = (String) reviewTask.get("content");
            log.info("内容：{}", content);
            ApplicationParam cParam = IntelligentAuditingUtil.getApplicationParam(APPLICATION_ID, content);

            Application application = new Application();
            ApplicationResult result = application.call(cParam);

            String reason = result.getOutput().getText();
            log.info("审核结果：{}", reason);

            Article article = new Article();
            article.setArticleId(articleId);
            ArticleReview articleReview = new ArticleReview();
            articleReview.setArticleId(articleId);
            articleReview.setReason(reason);
            articleReview.setReviewedAt(LocalDateTime.now());
            articleReviewMapper.updateByArticleId(articleReview);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("消息已确认并删除");

        } catch (Exception e) {
            e.printStackTrace();
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("消息拒绝失败", ex);
            }
        }

    }
}
