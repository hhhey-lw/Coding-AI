package com.coding.graph.core.node.impl;

import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.streaming.StreamingChatGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmNode implements NodeAction {

    public static final String LLM_RESPONSE_KEY = "llm_response";

    private String model;

    private String systemPrompt;

    private String userPrompt;

    private Map<String, Object> params = new HashMap<>();

    private List<Message> messages = new ArrayList<>();

    @Deprecated
    private List<Advisor> advisors = new ArrayList<>();

    private List<ToolCallback> toolCallbacks = new ArrayList<>();

    @Deprecated
    private String systemPromptKey;

    @Deprecated
    private String userPromptKey;

    @Deprecated
    private String paramsKey;

    private String messagesKey;

    private String outputKey;

    private ChatClient chatClient;

    private Boolean stream = Boolean.FALSE;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 初始化节点
        initNodeWithState(state);

        if (Boolean.TRUE.equals(stream)) {
            Flux<ChatResponse> chatResponseFlux = stream();
            var generator = StreamingChatGenerator.builder()
                    .startingNode("llmNode")
                    .startingState(state)
                    .mapResult(response -> Map.of(
                            StringUtils.isNotEmpty(this.outputKey) ? this.outputKey : "messages",
                            Objects.requireNonNull(response.getResult().getOutput())
                    ))
                    .build(chatResponseFlux);
            return Map.of(StringUtils.isNotEmpty(this.outputKey) ? this.outputKey : "messages", generator);
        }
        else {
            AssistantMessage responseOutput;
            try {
                ChatResponse response = call();
                responseOutput = response.getResult().getOutput();
            } catch (Exception e) {
                responseOutput = new AssistantMessage("调用异常：" + e.getMessage());
            }

            Map<String, Object> updatedState = new HashMap<>();
            updatedState.put("messages", responseOutput);
            if (StringUtils.isNotEmpty(this.outputKey)) {
                updatedState.put(this.outputKey, responseOutput);
            }
            return updatedState;
        }
    }

    private Flux<ChatResponse> stream() {
        return buildChatClientRequestSpec().stream().chatResponse();
    }

    private ChatResponse call() {
        return buildChatClientRequestSpec().call().chatResponse();
    }

    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec() {
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
                .options(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(false)
                        .model(model)
                        .maxTokens(2048)
                        .build())
                .messages(messages);

        if (CollectionUtils.isNotEmpty(toolCallbacks)) {
            chatClientRequestSpec.toolCallbacks(toolCallbacks);
        }

        if (CollectionUtils.isNotEmpty(advisors)) {
            chatClientRequestSpec.advisors(advisors);
        }

        if (StringUtils.isNotEmpty(systemPrompt)) {
            if (params != null && !params.isEmpty()) {
                systemPrompt = renderPromptTemplate(systemPrompt, params);
            }
            chatClientRequestSpec.system(systemPrompt);
        }
        if (StringUtils.isNotEmpty(userPrompt)) {
            if (!params.isEmpty()) {
                userPrompt = renderPromptTemplate(userPrompt, params);
            }
            chatClientRequestSpec.user(userPrompt);
        }
        return chatClientRequestSpec;
    }

    private void initNodeWithState(OverAllState state) {
        // 优先使用 state 中的值
        if (StringUtils.isNotEmpty(systemPromptKey)) {
            this.systemPrompt = (String) state.value(systemPromptKey).orElse(systemPrompt);
        }
        if (StringUtils.isNotEmpty(userPromptKey)) {
            this.userPrompt = (String) state.value(userPromptKey).orElse(userPrompt);
        }
        if (StringUtils.isNotEmpty(paramsKey)) {
            this.params = (Map<String, Object>) state.value(paramsKey).orElse(new HashMap<>());
        }
        if (StringUtils.isNotEmpty(messagesKey)) {
            this.messages = (List<Message>) state.value(messagesKey).orElse(this.messages);
        }
        // 渲染模板
        if (StringUtils.isNotEmpty(userPrompt) && !params.isEmpty()) {
            this.userPrompt = renderPromptTemplate(userPrompt, params);
        }
    }

    private String renderPromptTemplate(String prompt, Map<String, Object> params) {
        PromptTemplate promptTemplate = new PromptTemplate(prompt);
        return promptTemplate.render(params);
    }

}
