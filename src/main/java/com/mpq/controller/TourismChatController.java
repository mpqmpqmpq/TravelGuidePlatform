package com.mpq.controller;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/tourism")
public class TourismChatController {

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> tourismChat(@RequestParam String prompt, @RequestParam String chatId) throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId("3983dcadc48e447ea653fe6396c234aa")
                .apiKey(System.getenv("DASH_SCOPE_API_KEY"))
                .prompt(prompt)
                .sessionId(chatId)
                .topP(0.7)
                .temperature(0.2f)
                .build();
        Application application = new Application();
        Flowable<ApplicationResult> resultFlowable = application.streamCall(param);
        AtomicReference<StringBuffer> buffer = new AtomicReference<>(new StringBuffer());
        return Flux.from(resultFlowable.map(data -> {
            String text = data.getOutput().getText();
            if (text == null || text.trim().isEmpty()) {
                return ""; // 忽略空行
            }
            text = text.replace("data:", "").trim(); // 清理无效内容

            // 缓冲逻辑：累积到完整句子或段落后再发送
            buffer.get().append(text);
            if (buffer.get().toString().contains("\n") || buffer.get().length() >= 50) { // 按换行符或长度分割
                String result = buffer.get().toString();
                buffer.set(new StringBuffer()); // 清空缓冲区
                return result;
            }
            return ""; // 不足长度时暂不发送
        })).concatWith(Flux.defer(() -> {
            // 最后发送剩余的缓冲区内容
            String remaining = buffer.get().toString();
            return remaining.isEmpty() ? Flux.empty() : Flux.just(remaining);
        })).filter(text -> !text.isEmpty()); // 过滤掉空字符串
    }
}
