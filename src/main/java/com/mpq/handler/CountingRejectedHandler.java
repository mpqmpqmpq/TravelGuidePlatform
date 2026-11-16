package com.mpq.handler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 兜底方案：自定义拒绝策略，手动统计拒绝任务数
 */
public class CountingRejectedHandler implements RejectedExecutionHandler {
    // 原子类统计拒绝任务数（线程安全，避免并发计数错误）
    private final AtomicLong rejectedTaskCount = new AtomicLong(0);
    // 委托给原有拒绝策略（CallerRunsPolicy），保持业务逻辑一致
    private final RejectedExecutionHandler delegate = new ThreadPoolExecutor.CallerRunsPolicy();

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 拒绝任务时，计数+1
        rejectedTaskCount.incrementAndGet();
        // 执行原有拒绝逻辑（由提交任务的主线程执行，避免任务丢失）
        delegate.rejectedExecution(r, executor);
    }

    // 提供外部获取拒绝任务数的方法
    public long getRejectedTaskCount() {
        return rejectedTaskCount.get();
    }
}