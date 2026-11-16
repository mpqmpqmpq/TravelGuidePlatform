package com.mpq.service;


import com.mpq.mapper.MessageMapper;
import com.mpq.pojo.Message;
import com.mpq.pojo.PendingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessagesBatchAddService {

    private static final int BATCH_SIZE = 20;

    private final MessageMapper messageMapper;
    /**
     * 批量处理消息：入库成功后才确认这批消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void processBatchMessages( ConcurrentLinkedQueue<PendingMessage> buffer) {
        if (buffer.isEmpty()) {
            return;
        }

        // 取出一批 PendingMessage
        List<PendingMessage> batch = new ArrayList<>();
        for (int i = 0; i < BATCH_SIZE && !buffer.isEmpty(); i++) {
            PendingMessage pm = buffer.poll();
            if (pm != null) {
                batch.add(pm);
            }
        }
        if (batch.isEmpty()) {
            return;
        }
        // 提取消息实体列表
        List<Message> messages = batch.stream()
                .map(PendingMessage::getMessage)
                .collect(Collectors.toList());

        try {
            // 批量插入数据库
            int insertedCount = messageMapper.addBatchMessages(messages);
            int duplicatedCount = messages.size() - insertedCount;
            log.info("批量插入完成：成功 {} 条，重复忽略 {} 条", insertedCount, duplicatedCount);

            // 在当前事务提交后，确认这些消息
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (PendingMessage pm : batch) {
                        try {
                            pm.getChannel().basicAck(pm.getDeliveryTag(), false);
                            log.debug("确认消息成功: tag={}", pm.getDeliveryTag());
                        } catch (IOException e) {
                            log.error("确认消息失败: tag={}, error={}", pm.getDeliveryTag(), e.getMessage(), e);
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("批量处理异常", e);
            // 发生异常时将所有消息放回缓冲区（因为事务会回滚，这些消息相当于没有被处理）
            batch.forEach(buffer::offer);
            throw e; // 触发重试
        }
    }

}
