package com.mpq.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/rag")
public class RagController {
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private VectorStore vectorStore;

    @GetMapping(value = "chat", produces = "text/plain; charset=UTF-8")
    public String generation(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .call()
                .content();
    }
}
