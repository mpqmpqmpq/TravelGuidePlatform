package com.mpq.controller;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import com.mpq.service.ChatHistoryService;
import io.reactivex.Flowable;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Objects;




@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP")
@Slf4j
@RequiredArgsConstructor
public class  MCPController {
    private final ChatHistoryService chatHistoryService;
    private static final String APPID = "4507c2206cb14a8888bb45acba02b85a";

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> mcpChat(@RequestParam String prompt, @RequestParam String chatId)
            throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam
                .builder().appId(APPID).apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .prompt(prompt).sessionId(chatId).incrementalOutput(true).topP(0.8)
                .temperature(0.3f).build(); Application application = new Application();


        Flowable<ApplicationResult> resultFlowable = application.streamCall(param);

//        准备累积完整响应的缓冲区
        StringBuffer fullResponse = new StringBuffer();
        // 4. 流式响应处理
        return Flux.from(resultFlowable)
                // 从每个响应结果中提取 AI 生成的文本片段
                .map(result -> result.getOutput().getText())
                // 过滤掉 null 值，避免后续空指针异常
                .filter(Objects::nonNull)
                // 去除文本首尾空白字符
                .map(String::trim)
                // 过滤掉空字符串
                .filter(text -> !text.isEmpty())
                // 对每个有效的文本片段进行额外处理
                .doOnNext(chunk -> {
                    // 虽然 StringBuffer 本身线程安全，但多个 Reactor 线程可能同时调用 append，
                    // 为了确保复合操作的原子性，这里使用 synchronized 显式同步。
                    synchronized (fullResponse) {
                        fullResponse.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    chatHistoryService.saveChatHistory(chatId, prompt, fullResponse.toString());
                    log.debug("会话保存的聊天记录：{}，回复长度：{}",
                            chatId, fullResponse.length());
                });
    }
}