package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 开始节点
 * 工作流的入口节点，负责初始化流程上下文
 */
@Slf4j
@Component
public class StartNode extends AbstractNode {

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        log.info("工作流开始执行，节点ID: {}", node.getId());

        // 初始化上下文数据
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("startTime", System.currentTimeMillis());
        resultData.put("nodeId", node.getId());
        resultData.put("status", "started");

        // 将初始配置参数传递到上下文
        if (node.getConfigParams() != null) {
            resultData.putAll(node.getConfigParams());
        }

        return resultData;
    }

    @Override
    protected boolean doValidate(Node node) {
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.START.name();
    }
}
