package com.mpq.config;

import com.mpq.handler.CountingRejectedHandler;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    /**
     * 用户行为线程池
     */
    @Bean(name = "userBehaviorExecutor")
    public ThreadPoolTaskExecutor userBehaviorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 3);
        executor.setQueueCapacity(2000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("UserBehavior-");
        executor.setRejectedExecutionHandler(new CountingRejectedHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * 视频分片上传线程池
     */
    @Bean(name = "videoUploadExecutor")
    public ThreadPoolTaskExecutor videoUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2; // I/O密集型可适当调大

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 4);      // 最大线程数放宽
        executor.setQueueCapacity(200);                  // 有界队列，防止内存积压
        executor.setKeepAliveSeconds(120);                // 空闲线程存活时间稍长
        executor.setThreadNamePrefix("VideoUpload-");
        // 拒绝策略：由调用者线程执行（避免分片丢失，但会阻塞调用线程，需权衡）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 用户行为线程池监控指标
     */
    @Bean
    public Object registerUserBehaviorMetrics(
            MeterRegistry meterRegistry,
            @Qualifier("userBehaviorExecutor") ThreadPoolTaskExecutor userBehaviorExecutor) {

        ThreadPoolExecutor executor = userBehaviorExecutor.getThreadPoolExecutor();
        CountingRejectedHandler rejectedHandler = (CountingRejectedHandler) executor.getRejectedExecutionHandler();

        Gauge.builder("user_behavior_thread_pool_active_threads", executor, ThreadPoolExecutor::getActiveCount)
                .description("用户行为线程池当前活跃线程数")
                .register(meterRegistry);

        Gauge.builder("user_behavior_thread_pool_queue_size", executor.getQueue(), java.util.Queue::size)
                .description("用户行为线程池任务队列当前积压数")
                .register(meterRegistry);

        Gauge.builder("user_behavior_thread_pool_completed_tasks", executor, ThreadPoolExecutor::getCompletedTaskCount)
                .description("用户行为线程池累计完成任务总数")
                .register(meterRegistry);

        Gauge.builder("user_behavior_thread_pool_rejected_tasks", rejectedHandler, CountingRejectedHandler::getRejectedTaskCount)
                .description("用户行为线程池累计拒绝任务总数")
                .register(meterRegistry);
        return new Object();
    }

    /**
     * 视频分片上传线程池监控指标
     */
    @Bean
    public Object registerVideoUploadMetrics(
            MeterRegistry meterRegistry,
            @Qualifier("videoUploadExecutor") ThreadPoolTaskExecutor videoUploadExecutor) {

        ThreadPoolExecutor executor = videoUploadExecutor.getThreadPoolExecutor();

        // 1. 活跃线程数
        Gauge.builder("video_upload_thread_pool_active_threads", executor, ThreadPoolExecutor::getActiveCount)
                .description("视频上传线程池当前活跃线程数")
                .register(meterRegistry);

        // 2. 队列任务数
        Gauge.builder("video_upload_thread_pool_queue_size", executor.getQueue(), java.util.Queue::size)
                .description("视频上传线程池任务队列当前积压数")
                .register(meterRegistry);

        // 3. 累计完成任务数
        Gauge.builder("video_upload_thread_pool_completed_tasks", executor, ThreadPoolExecutor::getCompletedTaskCount)
                .description("视频上传线程池累计完成任务总数")
                .register(meterRegistry);

        return new Object();
    }
}