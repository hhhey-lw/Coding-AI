package com.coding.workflow;

import com.coding.workflow.manager.ModelExecuteManager;
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
class SpringAiWorkflowApplicationTests {

    @Resource
    private ModelExecuteManager modelExecuteManager;

    @Test
    void contextLoads() {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("你好，你的型号是？"));
        Mono<AgentResponse> chatResponse = modelExecuteManager.chat("BaiLian", "TextGen", "deepseek-v3", Map.of(), messages);
        chatResponse.doOnNext((response) -> {
            System.out.println(response.getMessage());
        }).subscribe();
    }

}
