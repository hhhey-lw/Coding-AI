package com.coding.agentflow.service.node;

import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 条件Agent节点
 * 基于Agent的智能决策进行条件分支判断
 */
@Slf4j
@Component
@AllArgsConstructor
public class ConditionAgentNode extends AbstractNode {

    private final ChatModel chatModel;

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String modelName = getConfigParamAsString(node, "modelName");
        // 需要一致的顺序
        List<String> scenarios = getConfigParamAsList(node, "scenarios");
        List<String> sceneDescriptions = getConfigParamAsList(node, "sceneDescriptions");

        log.info("执行条件Agent节点，模型: {}, 场景数量: {}", modelName, scenarios.size());

        // 使用Agent进行智能条件判断
        String selectedScene = evaluateConditionWithAgent(modelName, scenarios, sceneDescriptions);

        log.info("条件Agent节点选择的场景: {}", selectedScene);

        return NodeExecutionResult.success(Map.of("selectedScene", selectedScene));
    }

    /**
     * 使用Agent评估条件
     * 1. 构建Agent的prompt，包含上下文信息和所有场景
     * 2. 调用LLM让Agent分析并选择最合适的场景
     * 3. 验证返回的场景是否在候选列表中，否则抛异常
     *
     * @param modelName         模型名称
     * @param scenarios         场景列表
     * @param sceneDescriptions 场景描述
     * @return 选中的场景
     */
    private String evaluateConditionWithAgent(String modelName, List<String> scenarios, List<String> sceneDescriptions) {
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("场景列表不能为空");
        }

        // 构建场景列表，结合场景名称和描述
        StringBuilder scenarioListBuilder = new StringBuilder();
        for (int i = 0; i < scenarios.size(); i++) {
            String sceneName = scenarios.get(i);
            scenarioListBuilder.append("- ").append(sceneName);
            
            // 如果有对应的场景描述，添加到列表中
            if (sceneDescriptions != null && i < sceneDescriptions.size()) {
                String description = sceneDescriptions.get(i);
                if (StringUtils.isNotBlank(description)) {
                    scenarioListBuilder.append(": ").append(description);
                }
            }
            scenarioListBuilder.append("\n");
        }
        String scenarioList = scenarioListBuilder.toString();

        String userPrompt = "从以下场景列表中选择最合适的一个场景。\n\n" +
                "可选场景列表：\n" + scenarioList + "\n" +
                "请仔细分析场景描述，选择最匹配的场景。你的回答必须是场景名称（不包括描述），不要添加任何解释或其他内容，只返回场景名称。";

        // 构建系统提示词
        String finalSystemPrompt = "你是一个智能路由助手，擅长根据上下文信息选择最合适的场景。你必须从给定的场景列表中选择一个，并且只返回场景名称，不要有任何其他内容。";

        // 调用LLM进行场景选择
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        ChatResponse response = chatClient
                .prompt()
                .messages(List.of(
                        new SystemMessage(finalSystemPrompt),
                        new UserMessage(userPrompt)
                ))
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .build())
                .call()
                .chatResponse();

        String selectedScene = response.getResult().getOutput().getText().trim();
        log.info("LLM选择的场景: {}", selectedScene);

        // 验证选择的场景是否在候选列表中
        if (!scenarios.contains(selectedScene)) {
            throw new IllegalStateException(
                    String.format("LLM返回的场景 '%s' 不在候选列表中。候选场景: %s",
                            selectedScene, scenarios)
            );
        }

        return selectedScene;
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        List<String> scenarios = getConfigParamAsList(node, "scenarios");
        if (scenarios == null || scenarios.isEmpty()) {
            log.error("条件Agent节点缺少必需的scenarios配置");
            return false;
        }

        List<String> sceneDescriptions = getConfigParamAsList(node, "sceneDescriptions");
        if (sceneDescriptions == null || sceneDescriptions.isEmpty()) {
            log.error("条件Agent节点缺少必需的sceneDescriptions配置");
            return false;
        }

        String modelName = getConfigParamAsString(node, "modelName");
        if (StringUtils.isBlank(modelName)) {
            log.error("条件Agent节点缺少必需的modelName配置");
            return false;
        }

        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.CONDITION_AGENT.name();
    }
}
