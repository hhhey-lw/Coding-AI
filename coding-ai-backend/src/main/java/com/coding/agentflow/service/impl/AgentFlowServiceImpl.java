package com.coding.agentflow.service.impl;

import com.coding.agentflow.model.workflow.Edge;
import com.coding.agentflow.model.workflow.Node;
import com.coding.agentflow.model.workflow.WorkflowConfig;
import com.coding.agentflow.model.workflow.enums.NodeTypeEnum;
import com.coding.agentflow.service.AgentFlowNodeService;
import com.coding.agentflow.service.AgentFlowService;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactoryBuilder;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AgentFlowServiceImpl implements AgentFlowService {

    private final AgentFlowNodeService agentFlowNodeService;

    @Override
    public StateGraph convertToStateGraph(WorkflowConfig workflowConfig) throws GraphStateException {
        // 1. 创建StateGraph实例
        StateGraph stateGraph = new StateGraph(UUID.randomUUID().toString(), KeyStrategyFactoryBuilder.builder()
                .addStrategy("messages", KeyStrategy.APPEND)
                .build());

        // 2. 增加节点
        // 2.1 建立Node映射方便后续查找
        Map<String, Node> nodeMap = workflowConfig.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, node -> node));
        // 2.2 逐个添加节点
        for (Node node : workflowConfig.getNodes()) {
            try {
                stateGraph.addNode(node.getId(), agentFlowNodeService.getNodeActionWithConfig(node));
            } catch (Exception e) {
                throw new GraphStateException("添加节点失败，节点ID：" + node.getId());
            }
        }

        // 3. 增加边
        // 3.1 先按SourceNodeId对边进行分组
        Map<String, List<Edge>> edgesBySource = workflowConfig.getEdges().stream()
                .collect(Collectors.groupingBy(Edge::getSource));
        // 3.2 然后逐个处理每组边
        for (Map.Entry<String, List<Edge>> entry : edgesBySource.entrySet()) {
            String sourceId = entry.getKey();
            List<Edge> edges = entry.getValue();
            
            Node sourceNode = nodeMap.get(sourceId);
            if (sourceNode == null) {
                // 异常情况，源节点不存在
                throw new GraphStateException("添加边异常，因为节点不存在，NodeId：" + sourceId);
            }

            if (NodeTypeEnum.CONDITION.equals(sourceNode.getType()) || NodeTypeEnum.CONDITION_AGENT.equals(sourceNode.getType())) {
                // 处理条件边
                Map<String, String> routeMap = new HashMap<>();
                for (Edge edge : edges) {
                    // 条件边通过label确定目标节点
                    if (StringUtils.isBlank(edge.getLabel())) {
                        throw new GraphStateException("条件边的Label不能为空，from：" + sourceId + "，to：" + edge.getTarget());
                    }
                    routeMap.put(edge.getLabel(), edge.getTarget());
                }

                try {
                    // 代理Agent条件节点特殊处理
                    if (NodeTypeEnum.CONDITION_AGENT.equals(sourceNode.getType())) {
                        stateGraph.addConditionalEdges(sourceId,
                                AsyncEdgeAction.edge_async(state -> {
                                    // TODO 实现真正的条件逻辑
                                    // 输出routeMap的其中一个key
                                    System.out.println("Evaluating condition logic for node: " + sourceId);
                                    return "true";
                                }),
                                routeMap);
                    }
                    // 普通条件节点处理
                    else {
                        stateGraph.addConditionalEdges(sourceId,
                                AsyncEdgeAction.edge_async(state -> {
                                    // TODO 实现真正的条件逻辑
                                    // 输出routeMap的其中一个key
                                    System.out.println("Evaluating condition logic for node: " + sourceId);
                                    return "true";
                                }),
                                routeMap);
                    }

                } catch (Exception e) {
                    throw new GraphStateException("添加条件边失败，from：" + sourceId);
                }
            }
            else {
                // 处理普通边
                for (Edge edge : edges) {
                    try {
                        stateGraph.addEdge(edge.getSource(), edge.getTarget());
                    } catch (Exception e) {
                        throw new GraphStateException("添加边失败，from：" + edge.getSource() + "，to：" + edge.getTarget());
                    }
                }
            }
        }

        return stateGraph;
    }

}
