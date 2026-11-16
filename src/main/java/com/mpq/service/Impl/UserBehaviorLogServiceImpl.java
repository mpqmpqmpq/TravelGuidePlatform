package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mpq.pojo.PendingUserBehaviorLog;
import com.mpq.pojo.UserBehaviorLog;
import com.mpq.mapper.UserBehaviorLogMapper;
import com.mpq.service.UserBehaviorLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用户行为原始日志表 服务实现类
 *
 * @author mpq
 * @since 2026-05-05
 */
@Service
@RequiredArgsConstructor
public class UserBehaviorLogServiceImpl extends ServiceImpl<UserBehaviorLogMapper, UserBehaviorLog> implements UserBehaviorLogService {


    private static final int BATCH_SIZE = 20;
    @Override
    public boolean existsRecent(Integer userId, Integer targetId, Integer actionType, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);

        LambdaQueryWrapper<UserBehaviorLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBehaviorLog::getUserId, userId)
                .eq(UserBehaviorLog::getTargetId, targetId)
                .eq(UserBehaviorLog::getActionType, actionType)
                .ge(UserBehaviorLog::getCreatedAt, since);
        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBatchLog(ConcurrentLinkedQueue<PendingUserBehaviorLog> buffer) {

        if (buffer.isEmpty()) return;

        List<PendingUserBehaviorLog> pUBLogs = new ArrayList<>();

        for (int i = 0;i<BATCH_SIZE && !buffer.isEmpty();i++){
            PendingUserBehaviorLog poll = buffer.poll();
            pUBLogs.add(poll);
        }

        List<UserBehaviorLog> userBehaviorLogs = pUBLogs.stream().
                map(PendingUserBehaviorLog::getUserBehaviorLog).toList();

        try {
            this.saveBatch(userBehaviorLogs);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        for (PendingUserBehaviorLog pUBLog : pUBLogs) {
                            pUBLog.getChannel().basicAck(pUBLog.getDeliveryTag(), false);
                        }
                    }catch (Exception e){
                        log.error("日志消息确认失败");
                    }
                }
            });
        }catch (Exception e){
            log.error("行为日志批量处理异常", e);
            // 发生异常时将所有消息放回缓冲区
            pUBLogs.forEach(buffer::offer);
            throw e; // 触发重试
        }

    }
}
