package com.coding.agentflow.utils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式 Tool Call 合并工具类
 *
 * 解决 Qwen 等 OpenAI 兼容 API 在流式 tool call 时的 chunk 合并问题。
 *
 * 问题描述：
 * 某些 API（如 Qwen via OpenRouter）的流式响应模式：
 * - Chunk 1: id="tool-123", name="getCurrentWeather", args=""
 * - Chunk 2: id="", name="", args="{\"location\": \""
 * - Chunk 3: id="", name="", args="Seoul\"}"
 *
 * Spring AI 的 MessageAggregator 使用 addAll() 会创建多个不完整的 ToolCall 对象，
 * 导致 "toolName cannot be null or empty" 错误。
 *
 * 此工具类参考 Spring AI PR #4794 的修复方案，提供本地解决方案。
 *
 * @see <a href="https://github.com/spring-projects/spring-ai/pull/4794">PR #4794</a>
 */
public class StreamingToolCallMerger {

    /**
     * 合并流式 tool call chunks
     *
     * @param toolCalls 可能包含不完整 chunk 的 tool call 列表
     * @return 合并后的完整 tool call 列表
     */
    public static List<ToolCall> mergeToolCalls(List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return toolCalls;
        }

        // 使用 LinkedHashMap 保持顺序
        Map<String, ToolCall> mergedById = new LinkedHashMap<>();
        List<ToolCall> noIdCalls = new ArrayList<>();

        for (ToolCall toolCall : toolCalls) {
            String id = toolCall.id();

            if (id != null && !id.isEmpty()) {
                // 有 ID 的 tool call，按 ID 合并
                if (mergedById.containsKey(id)) {
                    ToolCall existing = mergedById.get(id);
                    mergedById.put(id, mergeToolCall(existing, toolCall));
                } else {
                    mergedById.put(id, toolCall);
                }
            } else {
                // 无 ID 的 tool call，收集起来后续处理
                noIdCalls.add(toolCall);
            }
        }

        // 处理无 ID 的 chunks
        if (!noIdCalls.isEmpty()) {
            if (mergedById.isEmpty()) {
                // 所有 chunks 都没有 ID，按顺序合并到第一个
                ToolCall merged = noIdCalls.get(0);
                for (int i = 1; i < noIdCalls.size(); i++) {
                    merged = mergeToolCall(merged, noIdCalls.get(i));
                }
                return List.of(merged);
            } else {
                // 有 ID 的 tool call 存在，将无 ID 的 chunks 合并到最后一个有 ID 的 tool call
                String lastId = null;
                for (String key : mergedById.keySet()) {
                    lastId = key;
                }
                if (lastId != null) {
                    ToolCall lastToolCall = mergedById.get(lastId);
                    for (ToolCall noIdCall : noIdCalls) {
                        lastToolCall = mergeToolCall(lastToolCall, noIdCall);
                    }
                    mergedById.put(lastId, lastToolCall);
                }
            }
        }

        return new ArrayList<>(mergedById.values());
    }

    /**
     * 合并两个 ToolCall 对象
     * 使用 null-safe 方式合并属性
     *
     * @param existing 已存在的 tool call
     * @param incoming 新的 tool call chunk
     * @return 合并后的 tool call
     */
    public static ToolCall mergeToolCall(ToolCall existing, ToolCall incoming) {
        String id = mergeString(existing.id(), incoming.id());
        String type = mergeString(existing.type(), incoming.type());
        String name = mergeString(existing.name(), incoming.name());
        String arguments = appendString(existing.arguments(), incoming.arguments());

        return new ToolCall(id, type, name, arguments);
    }

    /**
     * 合并字符串，优先使用非空值
     */
    private static String mergeString(String existing, String incoming) {
        if (incoming != null && !incoming.isEmpty()) {
            return incoming;
        }
        return existing;
    }

    /**
     * 追加字符串（用于 arguments 的累积）
     */
    private static String appendString(String existing, String incoming) {
        if (existing == null || existing.isEmpty()) {
            return incoming != null ? incoming : "";
        }
        if (incoming == null || incoming.isEmpty()) {
            return existing;
        }
        return existing + incoming;
    }

    /**
     * 聚合流式 ChatResponse，修复 Qwen 等 API 的 tool call 合并问题。
     *
     * 此方法替代 Spring AI 的 MessageAggregator，在聚合过程中正确合并 tool call chunks。
     *
     * @param chatResponseFlux 流式 ChatResponse
     * @return 聚合后的单个 ChatResponse（包含完整的 tool calls）
     */
    public static Flux<ChatResponse> aggregateWithToolCallMerging(Flux<ChatResponse> chatResponseFlux) {
        AtomicReference<StringBuilder> textContentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<List<ToolCall>> accumulatedToolCallsRef = new AtomicReference<>(new ArrayList<>());
        AtomicReference<ChatResponseMetadata> lastMetadataRef = new AtomicReference<>();
        AtomicReference<ChatGenerationMetadata> lastGenMetadataRef = new AtomicReference<>();
        AtomicReference<Map<String, Object>> lastMessageMetadataRef = new AtomicReference<>(new HashMap<>());

        return chatResponseFlux
                .doOnNext(response -> {
                    if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                        return;
                    }

                    Generation generation = response.getResult();
                    if (generation == null) {
                        return;
                    }

                    AssistantMessage output = generation.getOutput();
                    if (output != null) {
                        // 累积文本内容
                        if (output.getText() != null) {
                            textContentRef.get().append(output.getText());
                        }

                        // 累积 tool calls
                        if (output.hasToolCalls()) {
                            accumulatedToolCallsRef.get().addAll(output.getToolCalls());
                        }

                        // 保存消息元数据
                        if (output.getMetadata() != null && !output.getMetadata().isEmpty()) {
                            lastMessageMetadataRef.get().putAll(output.getMetadata());
                        }
                    }

                    // 保存响应元数据
                    if (response.getMetadata() != null) {
                        lastMetadataRef.set(response.getMetadata());
                    }

                    // 保存生成元数据
                    if (generation.getMetadata() != null) {
                        lastGenMetadataRef.set(generation.getMetadata());
                    }
                })
                .last()
                .map(lastResponse -> {
                    // 合并所有累积的 tool calls
                    List<ToolCall> mergedToolCalls = mergeToolCalls(accumulatedToolCallsRef.get());

                    // 创建最终的 AssistantMessage
                    AssistantMessage finalMessage = AssistantMessage.builder()
                            .content(textContentRef.get().toString())
                            .properties(lastMessageMetadataRef.get())
                            .toolCalls(mergedToolCalls)
                            .media(Collections.emptyList())
                            .build();

                    // 创建最终的 Generation
                    Generation finalGeneration = new Generation(finalMessage, lastGenMetadataRef.get());

                    // 构建最终的 ChatResponse
                    return ChatResponse.builder()
                            .generations(List.of(finalGeneration))
                            .metadata(lastMetadataRef.get())
                            .build();
                })
                .flux();
    }

    /**
     * 流式输出并同时检测 tool calls。
     *
     * 此方法会：
     * 1. 透传所有文本 chunks 给下游（实现真正的流式输出）
     * 2. 同时在后台聚合 tool calls
     * 3. 在流结束时，发送一个包含完整 tool calls 的聚合响应
     *
     * 这样用户可以看到 AI 的思考过程，同时系统也能检测到 tool calls。
     *
     * @param chatResponseFlux 原始流式响应
     * @return 透传的流式响应 + 最后一个聚合响应（包含完整 tool calls）
     */
    public static Flux<ChatResponse> streamAndAggregate(Flux<ChatResponse> chatResponseFlux) {
        AtomicReference<StringBuilder> textContentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<List<ToolCall>> accumulatedToolCallsRef = new AtomicReference<>(new ArrayList<>());
        AtomicReference<ChatResponseMetadata> lastMetadataRef = new AtomicReference<>();
        AtomicReference<ChatGenerationMetadata> lastGenMetadataRef = new AtomicReference<>();
        AtomicReference<Map<String, Object>> lastMessageMetadataRef = new AtomicReference<>(new HashMap<>());

        return chatResponseFlux
                // 透传每个 chunk，同时累积数据
                .doOnNext(response -> {
                    if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                        return;
                    }

                    Generation generation = response.getResult();
                    if (generation == null) {
                        return;
                    }

                    AssistantMessage output = generation.getOutput();
                    if (output != null) {
                        // 累积文本内容
                        if (output.getText() != null) {
                            textContentRef.get().append(output.getText());
                        }

                        // 累积 tool calls
                        if (output.hasToolCalls()) {
                            accumulatedToolCallsRef.get().addAll(output.getToolCalls());
                        }

                        // 保存消息元数据
                        if (output.getMetadata() != null && !output.getMetadata().isEmpty()) {
                            lastMessageMetadataRef.get().putAll(output.getMetadata());
                        }
                    }

                    // 保存响应元数据
                    if (response.getMetadata() != null) {
                        lastMetadataRef.set(response.getMetadata());
                    }

                    // 保存生成元数据
                    if (generation.getMetadata() != null) {
                        lastGenMetadataRef.set(generation.getMetadata());
                    }
                })
                // 在流结束后，追加一个聚合响应（包含完整的 tool calls）
                .concatWith(Flux.defer(() -> {
                    // 合并所有累积的 tool calls
                    List<ToolCall> mergedToolCalls = mergeToolCalls(accumulatedToolCallsRef.get());

                    // 创建最终的 AssistantMessage（标记为聚合结果）
                    Map<String, Object> metadata = new HashMap<>(lastMessageMetadataRef.get());
                    metadata.put("__aggregated__", true);  // 标记这是聚合结果

                    AssistantMessage finalMessage = AssistantMessage.builder()
                            .content(textContentRef.get().toString())
                            .properties(metadata)
                            .toolCalls(mergedToolCalls)
                            .media(Collections.emptyList())
                            .build();

                    // 创建最终的 Generation
                    Generation finalGeneration = new Generation(finalMessage, lastGenMetadataRef.get());

                    // 构建最终的 ChatResponse
                    ChatResponse aggregatedResponse = ChatResponse.builder()
                            .generations(List.of(finalGeneration))
                            .metadata(lastMetadataRef.get())
                            .build();

                    return Flux.just(aggregatedResponse);
                }));
    }

    /**
     * 检查 ChatResponse 是否是聚合结果（由 streamAndAggregate 生成）
     */
    public static boolean isAggregatedResponse(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return false;
        }
        Map<String, Object> metadata = response.getResult().getOutput().getMetadata();
        return metadata != null && Boolean.TRUE.equals(metadata.get("__aggregated__"));
    }
}
