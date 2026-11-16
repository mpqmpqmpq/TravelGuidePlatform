package com.mpq.controller;




import com.mpq.common.Result;
import com.mpq.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@RestController
@RequestMapping("/ai")
@Slf4j
public class ChatController {
    public static String CHAT = "chat";
    private final ChatClient chatClient;
    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    private final ChatHistoryService chatHistoryService;
    public ChatController(ChatModel chatModel, ChatHistoryService chatHistoryService) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultUser("user")
                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小m。")
//                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.chatHistoryService = chatHistoryService;
    }
    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId) {
        // 1. 获取历史记录
        List<String> history = chatHistoryService.loadChatHistory(chatId);
        if (!history.isEmpty())
        log.info("list:{}", history.get(1));
        // 2. 构建完整上下文
        String context = chatHistoryService.buildConversationContext(history, prompt);
        // 3. 准备内存管理
        ChatMemory chatMemory = chatMemories.computeIfAbsent(chatId, id -> new InMemoryChatMemory());
        // 4. 创建响应累积器
        StringBuilder fullResponse = new StringBuilder();
        log.info("chatId:{}", chatId);
        log.info("prompt:{}", prompt);
        // 5. 创建流
        return chatClient.prompt()
                .user(context)
                .advisors(a -> a
                                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                                .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                        )
                .stream()
                .content()
                // 6. 累积响应
                .doOnNext(fullResponse :: append)
                // 7. 结束是保存历史
                .doOnComplete( ()->{
                    if (!fullResponse.isEmpty()){
                        chatHistoryService.saveChatHistory(chatId, prompt, fullResponse.toString());
                        log.info("Saved chat history for session: {}, response length: {}",
                                chatId,fullResponse.length());
                    }
                });

    }
//    删除聊天记录
    @DeleteMapping("/deleteChatHistory/{chatId}")
    public Result deleteChatHistory(@PathVariable String chatId) {
        boolean b = chatHistoryService.deleteChatHistory(chatId);
        log.info("deleteChatHistory:{}", b);
        if (b){
            chatMemories.clear();
            return Result.success("删除成功",chatId);
        }else {
            return Result.failure(500,"删除失败");
        }
    }

}
