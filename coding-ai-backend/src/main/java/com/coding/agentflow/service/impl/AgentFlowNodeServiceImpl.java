package com.coding.agentflow.service.impl;

import com.coding.agentflow.model.workflow.Node;
import com.coding.agentflow.model.workflow.enums.NodeTypeEnum;
import com.coding.agentflow.service.AgentFlowNodeService;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentFlowNodeServiceImpl implements AgentFlowNodeService {

    @Override
    public AsyncNodeActionWithConfig getNodeActionWithConfig(Node node) {
        if (NodeTypeEnum.CONDITION.equals(node.getType())) {
            return (state, config) -> CompletableFuture.completedFuture(Map.of());
        }
        return (state, config) -> {
            // 示例实现：简单地将节点的配置信息打印出来，并返回一个完成的Future
            Map<String, Object> nodeConfig = node.getConfigParams();
            System.out.println("Executing node: " + node.getId() + " with config: " + nodeConfig);
            System.out.println("RunnableConfig: " + config);

            // 这里可以根据节点类型和配置执行不同的逻辑
            // ...

            return CompletableFuture.completedFuture(Map.of("node", node));
        };
    }

}
