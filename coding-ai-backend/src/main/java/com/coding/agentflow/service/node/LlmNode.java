package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
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
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String model = getConfigParamAsString(node, "model");
        String prompt = getConfigParamAsString(node, "prompt");
        String systemPrompt = getConfigParamAsString(node, "systemPrompt", "你是一个乐于助人的AI助手。");
        Double temperature = getConfigParamAsDouble(node, "temperature");
        Integer maxTokens = getConfigParamAsInteger(node, "maxTokens");

        log.info("执行LLM节点，模型: {}, Temperature: {}, MaxTokens: {}", model, temperature, maxTokens);

        // 替换提示词中的变量
        String finalPrompt = replaceTemplateWithVariable(prompt, context);
        String finalSystemPrompt = replaceTemplateWithVariable(systemPrompt, context);

        log.debug("最终用户提示词: {}", finalPrompt);

        // 调用LLM服务
        ChatResponse response = executeChatRequest(finalSystemPrompt, finalPrompt, model, temperature, maxTokens);
        
        // 提取结果和token使用情况
        String output = response.getResult().getOutput().getText();
        Map<String, Object> usage = extractUsage(response.getMetadata());

        // 构造结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("model", model);
        resultData.put("prompt", finalPrompt);
        resultData.put("systemPrompt", finalSystemPrompt);
        resultData.put("output", output);
        resultData.put("usage", usage);

        return NodeExecutionResult.success(resultData);
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
     * 执行聊天请求
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
