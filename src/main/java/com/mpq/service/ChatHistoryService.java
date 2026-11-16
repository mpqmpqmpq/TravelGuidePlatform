package com.mpq.service;

import java.util.List;

public interface ChatHistoryService {
//    加载对话历史
    List<String> loadChatHistory(String chatId);
//    保存对话历史
    void saveChatHistory(String chatId, String userInput, String aiResponse);
//    构建对话上下文
    String buildConversationContext(List<String> history, String newPrompt);
//    删除对话记录
    boolean deleteChatHistory(String chatId);
}
