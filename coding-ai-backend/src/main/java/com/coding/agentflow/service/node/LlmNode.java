package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.coding.graph.core.streaming.StreamingChatGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM节点
 * 调用大语言模型进行文本生成、对话等任务
 */
@Slf4j
@Component
public class LlmNode extends AbstractNode {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;

    public LlmNode(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        // 获取配置参数
        String model = getConfigParamAsString(node, "model");
        Double temperature = getConfigParamAsDouble(node, "temperature");
        Integer maxTokens = getConfigParamAsInteger(node, "maxTokens");
        Boolean stream = getConfigParamAsBoolean(node, "stream", false);
        // Memory 相关配置
        Boolean enableMemory = getConfigParamAsBoolean(node, "enableMemory", false);
        Integer memorySize = getConfigParamAsInteger(node, "memorySize", 100);
        // conversationId 从 state 获取，用于区分不同会话
        String conversationId = state.value("conversationId", "default-" + node.getId());
        
        List<Message> springMessages = getMessageList(node, state);

        log.info("执行LLM节点，模型: {}, Temperature: {}, MaxTokens: {}, Stream: {}, Memory: {}", 
                model, temperature, maxTokens, stream, enableMemory);

        // 判断是否使用流式模式
        if (Boolean.TRUE.equals(stream)) {
            return executeStreaming(node, springMessages, model, temperature, maxTokens, state, 
                    enableMemory, memorySize, conversationId);
        }

        // 执行聊天请求
        ChatResponse response = executeChatRequest(springMessages, model, temperature, maxTokens,
                enableMemory, memorySize, conversationId);
        String output = response.getResult().getOutput().getText();
        Map<String, Object> usage = extractUsage(response.getMetadata());

        // 直接返回结果数据
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("model", model);
        resultData.put("output", output);
        resultData.put("usage", usage);

        return resultData;
    }

    /**
     * 获取消息列表
     */
    private List<Message> getMessageList(Node node, OverAllState state) {
        List<Message> springMessages = new ArrayList<>();
        Object messagesObj = node.getConfigParams().get("messages");

        if (messagesObj instanceof List) {
            List<Map<String, String>> messagesConfig = (List<Map<String, String>>) messagesObj;
            for (Map<String, String> msgMap : messagesConfig) {
                String role = msgMap.get("role");
                String content = msgMap.get("content");
                String finalContent = replaceTemplateWithVariable(content, state);

                if ("system".equalsIgnoreCase(role)) {
                    springMessages.add(new SystemMessage(finalContent));
                } else if ("user".equalsIgnoreCase(role)) {
                    springMessages.add(new UserMessage(finalContent));
                } else if ("assistant".equalsIgnoreCase(role)) {
                    springMessages.add(new AssistantMessage(finalContent));
                }
            }
        }
        return springMessages;
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String model = getConfigParamAsString(node, "model");
        Object messages = node.getConfigParams().get("messages");
        String prompt = getConfigParamAsString(node, "prompt");
        
        if (messages == null && StringUtils.isBlank(prompt)) {
            log.error("LLM节点缺少必需的messages或prompt配置");
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
    private Map<String, Object> executeStreaming(Node node, List<Message> messages,
                                                String model, Double temperature, Integer maxTokens,
                                                OverAllState state, Boolean enableMemory, 
                                                Integer memorySize, String conversationId) {
        // 构建 ChatClient，根据配置决定是否添加 Memory Advisor
        ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);
        if (Boolean.TRUE.equals(enableMemory)) {
            clientBuilder.defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build()
            );
            log.info("[LlmNode] 启用 Memory Advisor, conversationId={}", conversationId);
        }
        ChatClient chatClient = clientBuilder.build();

        // 构建流式响应
        Flux<ChatResponse> chatResponseFlux = chatClient
                .prompt()
                .messages(messages)
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
                    finalResult.put("output", output);
                    finalResult.put("usage", usage);
                    return finalResult;
                })
                .build(chatResponseFlux);

        // 框架会自动识别 AsyncGenerator 并处理流式输出
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("output", generator);

        return resultData;
    }

    /**
     * 执行聊天请求（普通模式）
     */
    private ChatResponse executeChatRequest(List<Message> messages, String model, Double temperature, Integer maxTokens,
                                           Boolean enableMemory, Integer memorySize, String conversationId) {
        // 构建 ChatClient，根据配置决定是否添加 Memory Advisor
        ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);
        if (Boolean.TRUE.equals(enableMemory)) {
            clientBuilder.defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build()
            );
            log.info("[LlmNode] 启用 Memory Advisor, conversationId={}", conversationId);
        }
        ChatClient chatClient = clientBuilder.build();

        return chatClient
                .prompt()
                .messages(messages)
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
