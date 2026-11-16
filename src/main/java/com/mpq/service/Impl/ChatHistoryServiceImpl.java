package com.mpq.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpq.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {
    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY_PREFIX = "chat:history:";
    private static final TypeReference<List<String>> HISTORY_TYPE =
            new TypeReference<List<String>>() {
            };
    private static final ObjectMapper mapper = new ObjectMapper();
    @Value("${chat.history.max-rounds:10}")
    private int maxHistoryRounds;

    /**
     * 加载对话历史
     */
    public List<String> loadChatHistory(String chatId) {
        String key = REDIS_KEY_PREFIX + chatId;
        String historyJson = (String) redisTemplate.opsForValue().get(key);

        if (StringUtils.hasText(historyJson)) {
            try {
                return mapper.readValue(historyJson, HISTORY_TYPE);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse chat history for {}: {}", chatId, e.getMessage());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 保存对话历史
     */
    public void saveChatHistory(String chatId, String userInput, String aiResponse) {
        String key = REDIS_KEY_PREFIX + chatId;
        List<String> history = new ArrayList<>(loadChatHistory(chatId));

        // 添加新对话轮次
        history.add(userInput);
        history.add(aiResponse);

        // 限制历史记录长度（保留最近10轮对话）
        int maxEntries = maxHistoryRounds * 2;
        if (history.size() > maxEntries) {
            history = history.subList(history.size() - maxEntries, history.size());
        }

        try {
            redisTemplate.opsForValue().set(key, mapper.writeValueAsString(history));
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            log.error("Failed to save chat history for {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 构建对话上下文
     */
    public String buildConversationContext(List<String> history, String newPrompt) {
        StringBuilder context = new StringBuilder();

        // 添加历史上下文（交替的用户输入和AI回复）
        for (int i = 0; i < history.size(); i++) {
            if (i % 2 == 0) {
                context.append("User: ").append(history.get(i)).append("\n");
            } else {
                context.append("AI: ").append(history.get(i)).append("\n");
            }
        }

        // 添加当前问题
        context.append("User: ").append(newPrompt).append("\n");
        context.append("AI: ");

        return context.toString();
    }

    @Override
    public boolean deleteChatHistory(String chatId) {
        Boolean delete = redisTemplate.delete(REDIS_KEY_PREFIX + chatId);
        return delete;
    }
}

