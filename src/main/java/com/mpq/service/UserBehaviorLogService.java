package com.mpq.service;

import com.mpq.pojo.PendingUserBehaviorLog;
import com.mpq.pojo.UserBehaviorLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>
 * 用户行为原始日志表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-05-05
 */
public interface UserBehaviorLogService extends IService<UserBehaviorLog> {

    /**
     * 检查指定时间内是否已存在相同行为记录（去重）
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param actionType 行为类型
     * @param minutes    有效时间（分钟）
     * @return 是否存在
     */
    boolean existsRecent(Integer userId, Integer targetId, Integer actionType, int minutes);

    void addBatchLog(ConcurrentLinkedQueue<PendingUserBehaviorLog> buffer);
}
