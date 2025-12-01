package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeStatusEnum;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 人工输入节点
 * 暂停工作流执行，等待人工输入或审核
 */
@Slf4j
@Component
public class HumanInputNode extends AbstractNode {

    // TODO 逻辑有问题，并且key会冲突
    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String inputPrompt = getConfigParamAsString(node, "inputPrompt", "请输入内容");


        log.info("执行人工输入节点，提示: {}", inputPrompt);

        // 检查是否已有人工输入
        Object humanInput = getContextData(context, "humanInput" + node.getId());
        
        if (humanInput == null) {
            // 需要等待人工输入
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("inputPrompt", inputPrompt);
            resultData.put("status", NodeStatusEnum.PAUSED.name());

            NodeExecutionResult result = NodeExecutionResult.success(resultData);
            return result;
        } else {
            // 已收到人工输入
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("inputPrompt", inputPrompt);
            resultData.put("humanInput", humanInput);
            resultData.put("status", "completed");

            return NodeExecutionResult.success(resultData);
        }
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String inputPrompt = getConfigParamAsString(node, "inputPrompt", null);
        if (inputPrompt == null || inputPrompt.isEmpty()) {
            log.error("人工输入节点缺少必需的inputPrompt配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.HUMAN_INPUT.name();
    }
}
