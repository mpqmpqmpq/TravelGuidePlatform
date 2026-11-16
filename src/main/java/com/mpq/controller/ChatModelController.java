package com.mpq.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat2")
public class ChatModelController {
    private final ChatModel chatModel;

    public ChatModelController(@Qualifier("dashscopeChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(String input) {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .build();
        Prompt prompt = new Prompt(input, options);
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

}
