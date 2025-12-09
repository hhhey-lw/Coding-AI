package com.coding.agentflow.service.impl;

import com.coding.agentflow.model.model.Node;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.service.AgentFlowNodeService;
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

    // 条件节点不执行任何操作，直接返回空结果
    // 具体控制逻辑: AgentFlowServiceImpl#convertToStateGraph. 控制交由边来实现
    @Override
    public AsyncNodeActionWithConfig getNodeActionWithConfig(Node node) {
        if (NodeTypeEnum.CONDITION.equals(node.getType()) || NodeTypeEnum.CONDITION_AGENT.equals(node.getType())) {
            return (state, config) -> CompletableFuture.completedFuture(Map.of());
        }
        return (state, config) -> {
            try {
                // 获取节点执行器并执行
                NodeExecutor nodeExecutor = getNodeExecutor(node.getType().name());
                Map<String, Object> result = nodeExecutor.execute(node, state);
                
                // 直接返回执行结果，框架会自动识别其中的 AsyncGenerator
                return CompletableFuture.completedFuture(result != null ? result : Map.of());
            } catch (Exception e) {
                // 异常会被 CompletableFuture 包装，由框架处理
                return CompletableFuture.failedFuture(e);
            }
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
