package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.coding.graph.core.streaming.StreamingChatGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM节点
 * 调用大语言模型进行文本生成、对话等任务
 */
@Slf4j
@Component
@AllArgsConstructor
public class LlmNode extends AbstractNode {

    private final ChatModel chatModel;

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        // 获取配置参数
        String model = getConfigParamAsString(node, "model");
        String prompt = getConfigParamAsString(node, "prompt");
        String systemPrompt = getConfigParamAsString(node, "systemPrompt", "你是一个乐于助人的AI助手。");
        Double temperature = getConfigParamAsDouble(node, "temperature");
        Integer maxTokens = getConfigParamAsInteger(node, "maxTokens");
        Boolean stream = getConfigParamAsBoolean(node, "stream", false);

        log.info("执行LLM节点，模型: {}, Temperature: {}, MaxTokens: {}, Stream: {}", model, temperature, maxTokens, stream);

        // 替换提示词中的变量
        String finalPrompt = replaceTemplateWithVariable(prompt, state);
        String finalSystemPrompt = replaceTemplateWithVariable(systemPrompt, state);

        log.debug("最终用户提示词: {}", finalPrompt);

        // 判断是否使用流式模式
        if (Boolean.TRUE.equals(stream)) {
            return executeStreaming(node, finalSystemPrompt, finalPrompt, model, temperature, maxTokens, state);
        }

        // 执行聊天请求
        ChatResponse response = executeChatRequest(finalSystemPrompt, finalPrompt, model, temperature, maxTokens);
        String output = response.getResult().getOutput().getText();
        Map<String, Object> usage = extractUsage(response.getMetadata());

        // 直接返回结果数据
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("model", model);
        resultData.put("prompt", finalPrompt);
        resultData.put("systemPrompt", finalSystemPrompt);
        resultData.put("output", output);
        resultData.put("usage", usage);

        return resultData;
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String prompt = getConfigParamAsString(node, "prompt");
        String model = getConfigParamAsString(node, "model");
        if (StringUtils.isBlank(prompt)) {
            log.error("LLM节点缺少必需的prompt配置");
            return false;
        }
        if (StringUtils.isBlank(model)) {
            log.error("LLM节点缺少必需的model配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.LLM.name();
    }

    /**
     * 执行流式聊天请求
     */
    private Map<String, Object> executeStreaming(Node node, String finalSystemPrompt, String finalPrompt,
                                                String model, Double temperature, Integer maxTokens,
                                                OverAllState state) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 构建流式响应
        Flux<ChatResponse> chatResponseFlux = chatClient
                .prompt()
                .messages(List.of(
                        new SystemMessage(finalSystemPrompt),
                        new UserMessage(finalPrompt)
                ))
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .stream()
                .chatResponse();

        // 使用 StreamingChatGenerator 包装流式响应
        var generator = StreamingChatGenerator.builder()
                .startingNode(node.getId())
                .startingState(state)
                .mapResult(response -> {
                    // mapResult 在流式结束时调用，返回最终完整的结果
                    String output = response.getResult().getOutput().getText();
                    Map<String, Object> usage = extractUsage(response.getMetadata());
                    
                    Map<String, Object> finalResult = new HashMap<>();
                    finalResult.put("model", model);
                    finalResult.put("prompt", finalPrompt);
                    finalResult.put("systemPrompt", finalSystemPrompt);
                    finalResult.put("output", output);
                    finalResult.put("usage", usage);
                    return finalResult;
                })
                .build(chatResponseFlux);

        // 框架会自动识别 AsyncGenerator 并处理流式输出
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("output", generator); // 关键：直接用 output 作为 key

        return resultData;
    }

    /**
     * 执行聊天请求（普通模式）
     */
    private ChatResponse executeChatRequest(String finalSystemPrompt, String finalPrompt, String model, Double temperature, Integer maxTokens) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        return chatClient
                .prompt()
                .messages(List.of(
                        new SystemMessage(finalSystemPrompt),
                        new UserMessage(finalPrompt)
                ))
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .call()
                .chatResponse();
    }

    /**
     * 提取token使用情况
     */
    private Map<String, Object> extractUsage(ChatResponseMetadata metadata) {
        Map<String, Object> usage = new HashMap<>();
        
        if (metadata != null && metadata.getUsage() != null) {
            usage.put("promptTokens", metadata.getUsage().getPromptTokens() != null ? metadata.getUsage().getPromptTokens() : 0);
            usage.put("completionTokens", metadata.getUsage().getCompletionTokens() != null ? metadata.getUsage().getCompletionTokens() : 0);
            usage.put("totalTokens", metadata.getUsage().getTotalTokens() != null ? metadata.getUsage().getTotalTokens() : 0);
        } else {
            usage.put("promptTokens", 0);
            usage.put("completionTokens", 0);
            usage.put("totalTokens", 0);
        }
        
        return usage;
    }
}
