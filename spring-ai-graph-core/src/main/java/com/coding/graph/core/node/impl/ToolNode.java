package com.coding.graph.core.node.impl;

import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.state.OverAllState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolNode implements NodeAction {

    private String llmResponseKey;

    private String outputKey;

    private List<ToolCallback> toolCallbacks = new ArrayList<>();

    private AssistantMessage assistantMessage;

    private ToolCallbackResolver toolCallbackResolver;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        if (StringUtils.isBlank(llmResponseKey)) {
            this.llmResponseKey = LlmNode.LLM_RESPONSE_KEY;
        }

        // 获取LLM节点的结果
        this.assistantMessage = (AssistantMessage) state.value(this.llmResponseKey).orElseGet(() -> {
            if (state.value("messages").isPresent() && state.value("messages").get() instanceof Message message) {
                state.updateState(Map.of("messages", List.of(message)));
            }
            List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
            return messages.get(messages.size() - 1);
        });

        ToolResponseMessage toolResponseMessage = executeFunction(assistantMessage, state);

        Map<String, Object> updatedState = new HashMap<>();
        updatedState.put("messages", toolResponseMessage);
        if (StringUtils.isNotBlank(this.outputKey)) {
            updatedState.put(this.outputKey, toolResponseMessage);
        }
        return updatedState;
    }

    private ToolResponseMessage executeFunction(AssistantMessage assistantMessage, OverAllState state) {
        // 执行工具调用
        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            String toolName = toolCall.name();
            String toolArgs = toolCall.arguments();
            // 获取对应的工具
            ToolCallback toolCallback = this.resolve(toolName);
            String toolResult = toolCallback.call(toolArgs, new ToolContext(Map.of("state", state)));
            toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
        }
        return new ToolResponseMessage(toolResponses);
    }

    private ToolCallback resolve(String toolName) {
        return toolCallbacks.stream()
                .filter(callback -> callback.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseGet(() -> toolCallbackResolver.resolve(toolName));
    }
}
