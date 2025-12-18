package com.coding.core.service.impl;

import com.coding.core.model.model.ChatMessageModel;
import com.coding.core.repository.ChatMessageRepository;
import com.coding.core.service.ChatMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
                                yield AssistantMessage.builder()
                                        .content(text)
                                        .properties(metadata)
                                        .toolCalls(toolCalls)
                                        .build();
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
                                yield ToolResponseMessage.builder()
                                        .responses(responses)
                                        .metadata(metadata)
                                        .build();
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

    @Override
    public void removeIncompleteToolCalls(String conversationId) {
        if (StringUtils.isBlank(conversationId)) {
            return;
        }

        List<ChatMessageModel> messageModelList = chatMessageRepository.getByConversationId(conversationId);
        if (CollectionUtils.isEmpty(messageModelList)) {
            return;
        }

        // 收集需要删除的消息ID
        Set<Long> messageIdsToDelete = collectIncompleteToolCallMessageIds(messageModelList);

        if (CollectionUtils.isEmpty(messageIdsToDelete)) {
            return;
        }

        // 批量删除所有相关消息
        int removedCount = deleteMessagesBatch(messageIdsToDelete);
        log.info("Removed {} incomplete tool-call messages, conversationId={}", removedCount, conversationId);
    }

    /**
     * 批量删除消息
     */
    private int deleteMessagesBatch(Set<Long> messageIdsToDelete) {
        if (CollectionUtils.isEmpty(messageIdsToDelete)) {
            return 0;
        }

        return chatMessageRepository.deleteAll(messageIdsToDelete);
    }

    /**
     * 收集未完成工具调用的消息ID（包括ASSISTANT消息和对应的TOOL消息）
     */
    private Set<Long> collectIncompleteToolCallMessageIds(List<ChatMessageModel> messageModelList) {
        // toolCallId -> assistantMessageId
        Map<String, Long> toolCallIdToAssistantMessageId = new HashMap<>();
        // assistantMessageId -> assistantMessage对象
        Map<Long, ChatMessageModel> assistantMessageMap = new HashMap<>();
        // 记录每个ASSISTANT消息对应的TOOL消息ID
        Map<Long, Set<Long>> assistantToToolMessageIds = new HashMap<>();

        // 第一遍遍历：解析ASSISTANT消息，建立toolCall映射
        for (ChatMessageModel message : messageModelList) {
            if (!"ASSISTANT".equalsIgnoreCase(message.getType()) || !isValidMessage(message)) {
                continue;
            }

            try {
                JsonNode jsonNode = objectMapper.readTree(message.getMessages());
                if (jsonNode.has("toolCalls") && jsonNode.get("toolCalls").isArray()) {
                    Set<String> toolCallIds = new HashSet<>();

                    for (JsonNode toolCallNode : jsonNode.get("toolCalls")) {
                        String toolCallId = getStringValue(toolCallNode, "id");
                        if (StringUtils.isNotBlank(toolCallId)) {
                            toolCallIdToAssistantMessageId.put(toolCallId, message.getId());
                            toolCallIds.add(toolCallId);
                        }
                    }

                    if (!toolCallIds.isEmpty()) {
                        assistantMessageMap.put(message.getId(), message);
                        assistantToToolMessageIds.put(message.getId(), new HashSet<>());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse assistant message toolCalls: {}", maskSensitiveData(message.getMessages()), e);
            }
        }

        // 第二遍遍历：解析TOOL消息，标记已完成的toolCall
        for (ChatMessageModel message : messageModelList) {
            if (!"TOOL".equalsIgnoreCase(message.getType()) || !isValidMessage(message)) {
                continue;
            }

            try {
                JsonNode jsonNode = objectMapper.readTree(message.getMessages());
                if (jsonNode.has("responses") && jsonNode.get("responses").isArray()) {
                    for (JsonNode responseNode : jsonNode.get("responses")) {
                        String responseId = getStringValue(responseNode, "id");
                        if (StringUtils.isBlank(responseId)) {
                            continue;
                        }

                        Long assistantMessageId = toolCallIdToAssistantMessageId.remove(responseId);
                        if (assistantMessageId != null) {
                            // 从待删除的ASSISTANT消息中移除已完成的
                            assistantMessageMap.remove(assistantMessageId);

                            // 记录TOOL消息ID到对应关系中（用于后续删除）
                            assistantToToolMessageIds.computeIfAbsent(assistantMessageId, k -> new HashSet<>())
                                    .add(message.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse tool response message: {}", maskSensitiveData(message.getMessages()), e);
            }
        }

        // 收集所有需要删除的消息ID
        Set<Long> messageIdsToDelete = new HashSet<>();

        // 添加未完成的ASSISTANT消息
        messageIdsToDelete.addAll(assistantMessageMap.keySet());

        // 添加对应的TOOL消息
        for (Set<Long> toolMessageIds : assistantToToolMessageIds.values()) {
            messageIdsToDelete.addAll(toolMessageIds);
        }

        return messageIdsToDelete;
    }

    /**
     * 验证消息有效性
     */
    private boolean isValidMessage(ChatMessageModel message) {
        return message != null &&
                StringUtils.isNotBlank(message.getMessages()) &&
                StringUtils.isNotBlank(message.getType());
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : "";
    }

    /**
     * 脱敏敏感数据（防止日志泄露）
     */
    private String maskSensitiveData(String data) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        // 简单脱敏：如果数据较长，只显示前后部分
        if (data.length() > 100) {
            return data.substring(0, 50) + "..." + data.substring(data.length() - 20);
        }
        return data;
    }

}
