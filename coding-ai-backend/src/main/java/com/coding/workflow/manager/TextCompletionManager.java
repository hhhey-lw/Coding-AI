package com.coding.workflow.manager;

import cn.hutool.core.util.StrUtil;
import com.coding.workflow.enums.AgentStatusEnum;
import com.coding.workflow.enums.MessageRoleEnum;
import com.coding.workflow.model.chat.*;
import com.coding.workflow.model.factory.ModelFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class TextCompletionManager {

    @Resource
    private ModelFactory modelFactory;

    /**
     * 执行聊天
     */
    public Mono<AgentResponse> chat(String provider, String serviceType, String modelId, Map<String, Object> parameterMap,
                                    List<Message> messages) {
        ChatModel chatModel = modelFactory.getChatModel(provider, serviceType);

        OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder().model(modelId);
        ChatOptions chatOptions = buildChatOptions(parameterMap, chatOptionsBuilder);

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        Prompt prompt = new Prompt(messages);

        return Mono.fromCallable(() -> chatClient.prompt(prompt)
                        .options(chatOptions)
                        .call()
                        .chatResponse())
                .flatMap(this::convertResponse);
    }

    /**
     * 设置ChatClient参数
     */
    private static OpenAiChatOptions buildChatOptions(Map<String, Object> parameterMap, OpenAiChatOptions.Builder chatOptionsBuilder) {
        if (parameterMap != null) {
            if (parameterMap.get("temperate") != null && parameterMap.get("temperate") instanceof Double) {
                chatOptionsBuilder.temperature((Double) parameterMap.get("temperate"));
            }
            if (parameterMap.get("max_tokens") != null && parameterMap.get("max_tokens") instanceof Integer) {
                chatOptionsBuilder.maxTokens((Integer) parameterMap.get("max_tokens"));
            }
            if (parameterMap.get("top_p") != null && parameterMap.get("top_p") instanceof Double) {
                chatOptionsBuilder.topP((Double) parameterMap.get("top_p"));
            }
            if (parameterMap.get("presence_penalty") != null
                    && parameterMap.get("presence_penalty") instanceof Double) {
                chatOptionsBuilder.presencePenalty((Double) parameterMap.get("presence_penalty"));
            }
            if (parameterMap.get("frequency_penalty") != null
                    && parameterMap.get("frequency_penalty") instanceof Double) {
                chatOptionsBuilder.frequencyPenalty((Double) parameterMap.get("frequency_penalty"));
            }
            if (parameterMap.get("seed") != null && parameterMap.get("seed") instanceof Integer) {
                chatOptionsBuilder.seed((Integer) parameterMap.get("seed"));
            }
            if (parameterMap.get("response_format") != null && parameterMap.get("response_format") instanceof String) {
                chatOptionsBuilder.responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.valueOf(parameterMap.get("response_format").toString().toUpperCase()))
                        .build());
            }
        }
        return chatOptionsBuilder.build();
    }

    /**
     * 转换响应为 AgentResponse
     */
    protected Mono<AgentResponse> convertResponse(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return Mono.empty();
        }

        Usage usage = chatResponse.getMetadata().getUsage();

        AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
                .model(chatResponse.getMetadata().getModel())
                .modelUsage(ModelUsage.builder()
                        .promptTokens(usage.getPromptTokens())
                        .completionTokens(usage.getCompletionTokens())
                        .totalTokens(usage.getTotalTokens())
                        .build());

        if (!CollectionUtils.isEmpty(chatResponse.getResults())) {
            Generation generation = chatResponse.getResults().get(0);
            String finishReason = generation.getMetadata().getFinishReason();
            AgentStatusEnum status = AgentStatusEnum.toAgentStatus(finishReason);

            String reasoningContent = Optional.ofNullable(generation.getOutput().getMetadata().get("reasoningContent"))
                    .map(String::valueOf)
                    .filter(StrUtil::isNotBlank)
                    .orElse(null);

            ChatMessage.ChatMessageBuilder messageBuilder = ChatMessage.builder()
                    .role(MessageRoleEnum.ASSISTANT)
                    .content(generation.getOutput().getText())
                    .reasoningContent(reasoningContent);

            responseBuilder
                    .status(status)
                    .message(messageBuilder.build());
        }
        else {
            return Mono.empty();
        }

        return Mono.just(responseBuilder.build());
    }

}
