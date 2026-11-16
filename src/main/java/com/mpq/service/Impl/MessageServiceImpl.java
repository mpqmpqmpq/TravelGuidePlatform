package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.ConversationMapper;
import com.mpq.mapper.MessageMapper;
import com.mpq.pojo.Conversation;
import com.mpq.pojo.Message;
import com.mpq.service.ChatMessageConsumerService;
import com.mpq.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Random RANDOM = new Random();
    private final ConversationMapper conversationMapper;
    private final ChatMessageConsumerService chatMessageConsumerService;

    @Override
    public List<Message> getMessages(Integer conversationId, Integer userId, Integer pageNum, Integer size) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getConversationId, conversationId);
        Page<Message> page = new Page<>(pageNum, size);
        int offset = (pageNum - 1) * size;
        if (offset >= 100) {
            updateUnreadCount(conversationId, userId);
            this.page(page, queryWrapper);
            return page.getRecords();
        }
        // 优先从Redis获取
        String cacheKey = "messages:" + conversationId + ":page:" + pageNum + ":size:" + size;
        try {
            log.info("messages缓存获取开始");
            Object cachedMessages = redisTemplate.opsForValue().get(cacheKey);
            if (cachedMessages != null) {
                List<Message> messages = (List<Message>) cachedMessages;
                log.info("获取的第一个：{}", messages.get(0));
                updateUnreadCount(conversationId, userId);
                return messages;
            }
        } catch (Exception e) {
            log.warn("messages缓存获取失败: " + e.getMessage() + "准备从数据库中获取");
        }
        List<Message> messages = this.page(page, queryWrapper).getRecords();
        try {
            long randomTime = 5 + RANDOM.nextInt(5);
            redisTemplate.opsForValue().set(cacheKey, messages, randomTime, TimeUnit.MINUTES);
        } catch (Exception e) {

            log.warn("messages缓存失败: " + e.getMessage());
        }
        // 更新会话未读计数
        updateUnreadCount(conversationId, userId);
        return messages;
    }

    /**
     * 更新会话未读计数
     */
    private void updateUnreadCount(Integer conversationId, Integer userId) {

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCount1(0);
        } else {
            conversation.setUnreadCount2(0);
        }
        conversationMapper.updateById(conversation);
    }

    /**
     * 收到消息时更新消息缓存
     */
    @Override
    public void updateMessageCacheOnSend(Message message) {
        String cacheKey = "messages:" + message.getConversationId() + ":page:1:size:100";
        try {
            Object cachedMessages = redisTemplate.opsForValue().get(cacheKey);
            if (cachedMessages != null) {
                if (cachedMessages instanceof List) {
                    List<Message> messages = (List<Message>) cachedMessages;
                    log.info("现在第一个：{}", messages.get(0));
                    messages.add(0, message);
                    if (messages.size() > 100) {
                        messages = new ArrayList<>(messages.subList(0, 100));
                    }
                    log.info("更新消息缓存开始");
                    long randomTime = 5 + RANDOM.nextInt(5);
                    redisTemplate.opsForValue().set(cacheKey, messages, randomTime, TimeUnit.MINUTES);
                    List<Message> o = (List<Message>) redisTemplate.opsForValue().get(cacheKey);
                    log.info("修改后第一个：{}", o.get(0));
                }
            }
        } catch (Exception e) {
            redisTemplate.delete(cacheKey);
            log.warn("更新消息缓存失败: " + e.getMessage());

        }
    }

    @Override
    public void saveAllMessagesFromMQ() {
        try {
            chatMessageConsumerService.processAllBufferedMessages();
            log.info("已触发保存MQ中所有消息到数据库");
        } catch (Exception e) {
            log.error("触发保存MQ中所有消息到数据库失败", e);
        }
    }


}
