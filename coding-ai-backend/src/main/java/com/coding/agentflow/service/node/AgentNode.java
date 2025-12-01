package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent节点
 * 执行具有自主决策能力的智能代理任务
 */
@Slf4j
@Component
@AllArgsConstructor
public class AgentNode extends AbstractNode {

    private final ChatModel chatModel;

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数 - 需要支持ToolCall
        String chatModelName = getConfigParamAsString(node, "chatModel", "qwen-plus");
        String userPrompt = getConfigParamAsString(node, "prompt", "");
        // 知识库相关参数
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseIds");
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModelName = getConfigParamAsString(node, "rerankModel", "");
        // 工具参数
        List<String> tools = getConfigParamAsList(node, "tools");

        log.info("执行Agent节点，模型: {}, 知识库: {}, 工具: {}", chatModel, knowledgeBaseIds, tools);

        // TODO: 实际调用Agent服务
        // 补充系统提示词 - 1. 嵌入知识库碎片知识 2. 组合系统提示词&用户提示词
        String finalUserPrompt = replaceTemplateWithVariable(userPrompt, context);
        String finalSystemPrompt = queryKnowledgeBaseAndBuildPrompts(knowledgeBaseIds, embeddingModelName, rerankModelName, finalUserPrompt);

        // 构建ChatClient - 工具调用 & 思考内容
        ChatClient.ChatClientRequestSpec chatRequest = buildChatRequest(this.chatModel, chatModelName, tools, finalSystemPrompt, finalUserPrompt);


        // Agent通常会进行多轮思考和工具调用
        String output = chatRequest.call().content();

        // 包装返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("reasoning", "Agent推理过程（待实现）");
        resultData.put("answer", output);

        return NodeExecutionResult.success(resultData);
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String chatModel = getConfigParamAsString(node, "model", "qwen-plus");
        if (chatModel == null || chatModel.isEmpty()) {
            log.error("Agent节点缺少必需的chatModel配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.AGENT.name();
    }

    /**
     * 构建Chat请求
     */
    private ChatClient.ChatClientRequestSpec buildChatRequest(ChatModel chatModel, String chatModelName, List<String> tools, String systemPrompt, String userPrompt) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        List<ToolCallback> toolCallbacks = buildToolCallbacks(tools);

        return chatClient
                .prompt()
                .messages(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ))
                .options(OpenAiChatOptions.builder()
                        .model(chatModelName)
                        .internalToolExecutionEnabled(true)
                        .streamUsage(true)
                        .parallelToolCalls(true)
                        .build())
                .toolCallbacks(toolCallbacks);
    }

    /**
     * 替换提示词中的变量
     */
    private String replaceTemplateWithVariable(String userPrompt, Map<String, Object> context) {
        // TODO 实现变量替换逻辑
        return userPrompt;
    }


    /**
     * 检索模型知识库并构建系统提示词
     */
    private String queryKnowledgeBaseAndBuildPrompts(List<String> knowledgeBaseIds, String embeddingModelName, String rerankModelName, String finalUserPrompt) {
        // TODO
        return StringUtils.EMPTY;
    }

    /**
     * 构建工具回调列表
     * @param tools 工具名称列表
     * @return 工具回调列表
     */
    private List<ToolCallback> buildToolCallbacks(List<String> tools) {
        // TODO 构建工具列表
        return List.of();
    }

}
