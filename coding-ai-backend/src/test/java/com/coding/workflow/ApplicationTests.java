package com.coding.workflow;

import com.coding.workflow.manager.TextCompletionManager;
import com.coding.workflow.model.chat.AgentResponse;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
class ApplicationTests {

    @Resource
    private TextCompletionManager textCompletionManager;

    @Test
    void contextLoads() {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("你好，你的型号是？"));
        Mono<AgentResponse> chatResponse = textCompletionManager.chat("BaiLian", "TextGen", "deepseek-v3", Map.of(), messages);
        chatResponse.doOnNext((response) -> {
            System.out.println(response.getMessage());
        }).subscribe();
    }

}
