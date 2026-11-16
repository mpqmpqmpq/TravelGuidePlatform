package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.ConversationMapper;

import com.mpq.mapper.MessageMapper;
import com.mpq.mapper.NotificationsMapper;
import com.mpq.pojo.Conversation;
import com.mpq.pojo.Message;
import com.mpq.pojo.Notification;
import com.mpq.pojo.vo.ConversationWithUserVo;
import com.mpq.service.UserChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserChatServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements UserChatService {
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationsMapper notificationsMapper;

    private final static Integer NORMAL_STATUS = 0;
    private static final Random RANDOM = new Random();
    private static final Integer TARGET_TYPE_USER = 3;
    private static final Integer ACTION_TYPE_MESSAGE = 5;

    /**
     * 创建或获取会话
     */
    public Conversation getORCreateConversation(Integer user1Id, Integer user2Id) {
        if (user1Id > user2Id) {
            Integer temp = user1Id;
            user1Id = user2Id;
            user2Id = temp;
        }
        String cacheKey = "conversation:" + user1Id + ":" + user2Id;
        try {
            log.info("conversation缓存获取开始");
            Object cachedConversation = redisTemplate.opsForValue().get(cacheKey);
            if (cachedConversation != null) {
                return (Conversation) cachedConversation;
            }
        } catch (Exception e) {
            log.warn("conversation缓存获取失败: " + e.getMessage());
        }
        Conversation conversation = conversationMapper.findByUserIds(user1Id, user2Id);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUser1Id(user1Id);
            conversation.setUser2Id(user2Id);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.insert(conversation);
            clearUserConversationsCache(user1Id);
            clearUserConversationsCache(user2Id);
        }
        try {
            long randomTime = 25 + RANDOM.nextInt(5);
            redisTemplate.opsForValue().set(cacheKey, conversation, randomTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("conversation缓存失败: " + e.getMessage());
        }
        return conversation;
    }

    @Override
    public Message sendMessage(Integer senderId, Integer receiverId, String content) {

        Conversation conversation = getORCreateConversation(senderId, receiverId);
        Message message = new Message();
        message.setConversationId(conversation.getConversationId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setStatus(NORMAL_STATUS);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
//        更新会话的最后消息id和未读计数
        conversation.setLastMessageId(message.getMessageId());
        if (senderId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCount2(conversation.getUnreadCount2() + 1);
        } else {
            conversation.setUnreadCount1(conversation.getUnreadCount1() + 1);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        createBNotification(message);
        return message;
    }

    private void createBNotification(Message message) {
        try {
            Notification notification = new Notification();
            notification.setRecipientId(message.getReceiverId());
            notification.setSenderId(message.getSenderId());
            notification.setTargetType(TARGET_TYPE_USER); // 目标类型：3=用户
            notification.setTargetId(message.getSenderId()); // 目标 ID 为发送者 ID
            notification.setActionType(ACTION_TYPE_MESSAGE); // 操作类型：5=私信消息
            notification.setIsRead(false); // 未读状态
            notification.setCreatedAt(LocalDateTime.now());

            notificationsMapper.insert(notification);
            log.info("已为消息创建通知：接收者={}, 发送者={}", message.getReceiverId(), message.getSenderId());

        } catch (Exception e) {
            log.error("创建通知失败：", e);
        }
    }


    /**
     * 获取用户会话列表
     */
    @Override
    public List<Conversation> getUserConversations(Integer userId) {
        // 优先从Redis获取
        String cacheKey = "user_conversations:" + userId;
        try {
            log.info("user_conversations缓存获取开始");
            Object cachedConversations = redisTemplate.opsForValue().get(cacheKey);
            if (cachedConversations != null) {
                return (List<Conversation>) cachedConversations;
            }
        } catch (Exception e) {
            // 缓存读取失败，继续执行数据库查询
            log.warn("user_conversations缓存获取失败: " + e.getMessage());
        }

        LambdaQueryWrapper<Conversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Conversation::getUser1Id, userId)
                .or()
                .eq(Conversation::getUser2Id, userId)
                .orderByDesc(Conversation::getUpdatedAt);

        List<Conversation> conversations = conversationMapper.selectList(queryWrapper);

        // 存入Redis缓存
        try {
            long expireTime = 15 + RANDOM.nextInt(10); // 15-24分钟随机过期时间
            redisTemplate.opsForValue().set(cacheKey, conversations, expireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存存储失败，不影响主流程
        }
        return conversations;
    }

    @Override
    public List<ConversationWithUserVo> selectConversationsWithUsers(Integer userId) {
        String cacheKey = "user_conversationsWithUsers:" + userId;
        try {
            log.info("user_conversationsWithUsers缓存获取开始");
            Object cachedConversationsWithUsers = redisTemplate.opsForValue().get(cacheKey);
            if (cachedConversationsWithUsers != null) {
                return (List<ConversationWithUserVo>) cachedConversationsWithUsers;
            }
        } catch (Exception e) {
            log.warn("user_conversationsWithUsers缓存获取失败: " + e.getMessage());
        }
        List<ConversationWithUserVo> conversationsWithUsers = conversationMapper.selectConversationsWithUsers(userId);
        try {
            long expireTime = 15 + RANDOM.nextInt(10);
            redisTemplate.opsForValue().set(cacheKey, conversationsWithUsers, expireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("user_conversationsWithUsers缓存失败: " + e.getMessage());
        }
        return conversationsWithUsers;
    }


    private void clearUserConversationsCache(Integer userId) {
        String cacheKey = "user_conversationsWithUsers:" + userId;
        redisTemplate.delete(cacheKey);
    }


}
