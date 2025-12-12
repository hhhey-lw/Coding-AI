package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 结束节点
 * 工作流的出口节点，负责汇总流程结果
 */
@Slf4j
@Component
public class EndNode extends AbstractNode {

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        log.info("工作流执行结束，节点ID: {}", node.getId());

        // 汇总最终结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("endTime", System.currentTimeMillis());
        resultData.put("nodeId", node.getId());
        resultData.put("status", "completed");

        // 从上下文中提取最终结果
        String finalResult = getConfigParamAsString(node, "finalResult");
        if (StringUtils.isNotBlank(finalResult)) {
            String templateWithVariable = replaceTemplateWithVariable(finalResult, state);
            resultData.put("finalResult", templateWithVariable);
        }

        // 计算总执行时间
        Object startTime = state.value("startTime").orElse(null);
        if (startTime instanceof Long) {
            long duration = System.currentTimeMillis() - (Long) startTime;
            resultData.put("totalDuration", duration);
        }

        return resultData;
    }

    @Override
    protected boolean doValidate(Node node) {
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.END.name();
    }
}
