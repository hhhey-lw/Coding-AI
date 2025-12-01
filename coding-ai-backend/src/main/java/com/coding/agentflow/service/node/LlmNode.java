package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM节点
 * 调用大语言模型进行文本生成、对话等任务
 */
@Slf4j
@Component
public class LlmNode extends AbstractNode {

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String model = getConfigParamAsString(node, "model", "qwen-plus");
        String prompt = getConfigParamAsString(node, "prompt", "");

        log.info("执行LLM节点，模型: {}, Prompt: {}", model, prompt);

        // TODO: 实际调用LLM服务


        // 构造结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("model", model);
        resultData.put("prompt", prompt);
        resultData.put("output", "暂未实现");
        resultData.put("usage", Map.of(
                "promptTokens", 0,
                "completionTokens", 0,
                "totalTokens", 0
        ));

        return NodeExecutionResult.success(resultData);
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String prompt = getConfigParamAsString(node, "prompt", null);
        String model = getConfigParamAsString(node, "model", null);
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
}
