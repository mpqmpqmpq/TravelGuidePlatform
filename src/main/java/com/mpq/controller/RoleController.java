package com.mpq.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/role")
public class RoleController {
    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;
    private final ChatClient chatClient;

    public RoleController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
//                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小m。")
//                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
//                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @GetMapping("/roles")
    public AssistantMessage generate(
            @RequestParam(value = "message", defaultValue = "请写一首诗") String message,
            @RequestParam(value = "name", defaultValue = "Fox") String name,
            @RequestParam(value = "voice", defaultValue = "诗人") String voice
    ) {
        Assert.notNull(message, "消息内容不能为空");
        UserMessage userMessage = new UserMessage(message);
        log.info("userMessage:{}", userMessage);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", name, "voice", voice));
        Assert.notNull(systemMessage.getText(), "系统消息内容不能为空");
        log.info("systemMessage:{}", systemMessage.getText());
        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
        log.info("prompt:{}", prompt);

        return chatClient.prompt(prompt)
                .user(message)
                .call()
                .chatResponse()
                .getResult()
                .getOutput();
    }
}
