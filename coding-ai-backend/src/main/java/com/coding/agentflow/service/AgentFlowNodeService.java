package com.coding.agentflow.service;

import com.coding.agentflow.model.workflow.Node;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;

public interface AgentFlowNodeService {

    AsyncNodeActionWithConfig getNodeActionWithConfig(Node node);
}
