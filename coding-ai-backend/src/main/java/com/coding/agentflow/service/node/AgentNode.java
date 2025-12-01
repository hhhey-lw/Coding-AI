package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
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
        // 知识库相关参数
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseIds");
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModelName = getConfigParamAsString(node, "rerankModel", "");
        // 工具参数
        List<String> tools = getConfigParamAsList(node, "tools");

        log.info("执行Agent节点，模型: {}, 知识库: {}, 工具: {}", chatModel, knowledgeBaseIds, tools);

        // TODO: 实际调用Agent服务
        // 构建ChatClient
        ChatClient chatClient = buildChatClient(this.chatModel, chatModelName, tools);
        // Agent通常会进行多轮思考和工具调用

        // 包装返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("reasoning", "Agent推理过程（待实现）");
        resultData.put("answer", "Agent执行结果（待实现）");

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
     * 构建ChatClient
     *
     * @param chatModel
     * @param chatModelName
     * @param tools
     * @return
     */
    private ChatClient buildChatClient(ChatModel chatModel, String chatModelName, List<String> tools) {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder()
                        .model(chatModelName)
                        .build())
                .build();
        // TODO 1. 补充系统和用户提示词 2. 补充知识库嵌入 3. 补充工具调用 4. 补充思考链逻辑
        chatClient
                .prompt()
                .advisors()
                .toolCallbacks()
                .stream()
    }

}
