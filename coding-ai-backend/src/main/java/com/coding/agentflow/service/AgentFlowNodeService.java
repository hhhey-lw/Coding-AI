package com.coding.agentflow.service;

import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;

public interface AgentFlowNodeService {

    /**
     * 根据节点获取对应的执行动作
     *
     * @param node
     * @return
     */
    AsyncNodeActionWithConfig getNodeActionWithConfig(Node node);
}
