package com.coding.agentflow.service.impl;

import com.coding.agentflow.model.model.Node;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.service.AgentFlowNodeService;
import com.coding.agentflow.service.node.NodeExecutionResult;
import com.coding.agentflow.service.node.NodeExecutor;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentFlowNodeServiceImpl implements AgentFlowNodeService {

    @Resource
    private Map<String, NodeExecutor> nodeExecutorMap;

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
            NodeExecutor nodeExecutor = getNodeExecutor(node.getType().name());
            NodeExecutionResult execute = nodeExecutor.execute(node, state.data());

            // 返回节点执行结果 <= 这里会把返回的结果塞到 OverAllState中
            return CompletableFuture.completedFuture(Map.of(node.getId(), execute));
        };
    }

    /**
     * 根据节点类型获取对应的节点执行器
     */
    private NodeExecutor getNodeExecutor(String nodeType) {
        for (Map.Entry<String, NodeExecutor> entry : nodeExecutorMap.entrySet()) {
            if (entry.getValue().getNodeType().equals(nodeType)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException(String.format("节点类型不存在: {}", nodeType));
    }

}
