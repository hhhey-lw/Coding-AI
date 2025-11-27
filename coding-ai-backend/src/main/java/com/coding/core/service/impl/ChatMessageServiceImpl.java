package com.coding.core.service.impl;

import com.coding.core.model.model.ChatMessageModel;
import com.coding.core.repository.ChatMessageRepository;
import com.coding.core.service.ChatMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 聊天消息服务实现类
 * @author coding
 */
@Slf4j
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageRepository chatMessageRepository;

    @Resource
    private ObjectMapper objectMapper;


    @Override
    public List<Message> findMessages(String conversationId) {
        if (StringUtils.isBlank(conversationId)) {
            return Collections.emptyList();
        }
        List<ChatMessageModel> chatMessageModels = chatMessageRepository.getByConversationId(conversationId);
        return chatMessageModels.stream()
                .filter(chatMessageModel -> StringUtils.isNotBlank(chatMessageModel.getMessages()))
                .<Message>map(chatMessageModel -> {
                    String type = chatMessageModel.getType().toUpperCase();
                    String messageJson = chatMessageModel.getMessages();

                    try {
                        JsonNode jsonNode = objectMapper.readTree(messageJson);
                        String text = jsonNode.has("text") ? jsonNode.get("text").asText() : "";
                        
                        // 解析 metadata
                        Map<String, Object> metadata = new HashMap<>();
                        if (jsonNode.has("metadata") && jsonNode.get("metadata").isObject()) {
                            JsonNode metadataNode = jsonNode.get("metadata");
                            metadataNode.fields().forEachRemaining(entry -> {
                                metadata.put(entry.getKey(), entry.getValue().asText());
                            });
                        }

                        return switch (type) {
                            case "ASSISTANT" -> {
                                // 解析 toolCalls
                                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
                                if (jsonNode.has("toolCalls") && jsonNode.get("toolCalls").isArray()) {
                                    for (JsonNode toolCallNode : jsonNode.get("toolCalls")) {
                                        String id = toolCallNode.has("id") ? toolCallNode.get("id").asText() : "";
                                        String callType = toolCallNode.has("type") ? toolCallNode.get("type").asText() : "function";
                                        String name = toolCallNode.has("name") ? toolCallNode.get("name").asText() : "";
                                        String arguments = toolCallNode.has("arguments") ? toolCallNode.get("arguments").asText() : "";
                                        toolCalls.add(new AssistantMessage.ToolCall(id, callType, name, arguments));
                                    }
                                }
                                yield new AssistantMessage(text, metadata, toolCalls);
                            }
                            case "TOOL" -> {
                                // 解析 responses
                                List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
                                if (jsonNode.has("responses") && jsonNode.get("responses").isArray()) {
                                    for (JsonNode responseNode : jsonNode.get("responses")) {
                                        String id = responseNode.has("id") ? responseNode.get("id").asText() : "";
                                        String name = responseNode.has("name") ? responseNode.get("name").asText() : "";
                                        String responseData = responseNode.has("responseData") ? responseNode.get("responseData").asText() : "";
                                        responses.add(new ToolResponseMessage.ToolResponse(id, name, responseData));
                                    }
                                }
                                yield new ToolResponseMessage(responses, metadata);
                            }
                            case "USER" -> new UserMessage(text);
                            case "SYSTEM" -> new SystemMessage(text);
                            default -> {
                                log.warn("Unknown message type: {}, treating as user message", type);
                                yield new UserMessage(text);
                            }
                        };
                    } catch (Exception e) {
                        log.error("Failed to deserialize message: {}", messageJson, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void saveMessages(String conversationId, List<Message> messages) {
        List<ChatMessageModel> messageModels = messages.stream()
                .map(message -> {
                    try {
                        return ChatMessageModel.builder()
                                .conversationId(conversationId)
                                .messages(objectMapper.writeValueAsString(message))
                                .type(message.getMessageType().getValue())
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to serialize message: {}", message, e);
                        return null;
                    }
                })
                .filter(model -> model != null)
                .toList();
        chatMessageRepository.add(messageModels);
    }
}
