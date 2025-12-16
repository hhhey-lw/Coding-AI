package com.coding.agentflow.service.impl;

import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.entity.AgentFlowInstance;
import com.coding.agentflow.model.entity.AgentFlowNodeInstance;
import com.coding.agentflow.model.model.Branch;
import com.coding.agentflow.model.model.Edge;
import com.coding.agentflow.model.model.Node;
import com.coding.agentflow.model.model.AgentFlowConfig;
import com.coding.agentflow.model.enums.FlowStatusEnum;
import com.coding.agentflow.model.enums.NodeStatusEnum;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.repository.AgentFlowInstanceRepository;
import com.coding.agentflow.service.AgentFlowNodeService;
import com.coding.agentflow.service.AgentFlowService;
import com.coding.agentflow.service.node.ConditionAgentNode;
import com.coding.agentflow.utils.ConditionEvaluator;
import com.coding.graph.core.common.NodeCodeConstants;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.GraphLifecycleListener;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactoryBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AgentFlowServiceImpl implements AgentFlowService {

    private final AgentFlowNodeService agentFlowNodeService;
    private final ConditionAgentNode conditionAgentNode;
    private final ObjectMapper objectMapper;
    private final AgentFlowInstanceRepository agentFlowInstanceRepository;

    @Override
    public CompiledGraph convertToCompiledGraph(AgentFlowConfig agentFlowConfig) throws GraphStateException {
        // 1. 转换为StateGraph
        StateGraph stateGraph = convertToStateGraph(agentFlowConfig);
        // 2. 编译StateGraph为CompiledGraph
        return compiledGraph(stateGraph, agentFlowConfig);
    }

    /**
     * 转换WorkflowConfig为StateGraph
     *
     * @param agentFlowConfig 工作流配置
     * @return StateGraph 状态图
     * @throws GraphStateException 图状态异常
     */
    private StateGraph convertToStateGraph(AgentFlowConfig agentFlowConfig) throws GraphStateException {
        // 1. 创建StateGraph实例
        StateGraph stateGraph = new StateGraph(UUID.randomUUID().toString(), KeyStrategyFactoryBuilder.builder()
                .addStrategy("messages", KeyStrategy.APPEND)
                .build());

        // 2. 增加节点
        // 2.1 建立Node映射方便后续查找
        Map<String, Node> nodeMap = agentFlowConfig.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, node -> node));
        // 2.2 逐个添加节点
        for (Node node : agentFlowConfig.getNodes()) {
            try {
                stateGraph.addNode(node.getId(), agentFlowNodeService.getNodeActionWithConfig(node));
            } catch (Exception e) {
                throw new GraphStateException("添加节点失败，节点ID：" + node.getId());
            }
        }

        // 3. 增加边
        // 3.1 先按SourceNodeId对边进行分组
        Map<String, List<Edge>> edgesBySource = agentFlowConfig.getEdges().stream()
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
                    // 构建 label -> target 映射（LangGraph的条件边是异步动作，会返回一个label，框架会根据label执行对应label的边）
                    if (StringUtils.isBlank(edge.getTarget())) {
                        throw new GraphStateException("条件边的重点不能为空，from：" + sourceId + "，to：" + edge.getTarget());
                    }
                    // 标签 映射到 目标节点ID
                    routeMap.put(edge.getLabel(), edge.getTarget());
                }

                try {

                    stateGraph.addConditionalEdges(sourceId,
                            AsyncEdgeAction.edge_async((OverAllState state) -> {
                                System.out.println("Evaluating condition logic for node: " + sourceId);

                                if (NodeTypeEnum.CONDITION_AGENT.equals(sourceNode.getType())) {
                                    // 利用条件Agent节点进行智能评估
                                    String selectedLabel = conditionAgentNode.getSelectedLabel(sourceId, conditionAgentNode.execute(nodeMap.get(sourceId), state));
                                    log.info("Condition Agent Node selected label: {}", selectedLabel);
                                    return selectedLabel;
                                }

                                // 解析分支配置
                                Object branchesObj = sourceNode.getConfigParams() != null ? sourceNode.getConfigParams().get("branches") : null;
                                List<Branch> branches;
                                if (branchesObj != null) {
                                    branches = objectMapper.convertValue(branchesObj, new TypeReference<List<Branch>>() {
                                    });
                                } else {
                                    branches = List.of();
                                }

                                // 遍历分支，找到第一个满足条件的分支
                                for (Branch branch : branches) {
                                    if (ConditionEvaluator.evaluateBranch(branch, (Map<String, Object>) state.data())) {
                                        return branch.getLabel();
                                    }
                                }

                                // 如果没有匹配的分支，抛出异常
                                throw new RuntimeException("No matching branch found for condition node: " + sourceId);
                            }),
                            routeMap);

                } catch (Exception e) {
                    throw new GraphStateException("添加条件边失败，from：" + sourceId);
                }
            } else {
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

        // 4. 添加起始节点（END节点是保留标识符，不需要显式添加）
        stateGraph.addNode(NodeCodeConstants.START, ((state, config) -> CompletableFuture.completedFuture(config.getMetadata())));

        // 5. 添加起始边和结束边
        Node startNode = agentFlowConfig.getNodes().stream().filter(node -> NodeTypeEnum.START.equals(node.getType())).findFirst().orElseThrow(() -> new GraphStateException("起始节点不存在"));
        Node endNode = agentFlowConfig.getNodes().stream().filter(node -> NodeTypeEnum.END.equals(node.getType())).findFirst().orElseThrow(() -> new GraphStateException("结束节点不存在"));
        stateGraph.addEdge(NodeCodeConstants.START, startNode.getId());
        stateGraph.addEdge(endNode.getId(), NodeCodeConstants.END);

        return stateGraph;
    }

    /**
     * 编译StateGraph为CompiledGraph
     *
     * @param stateGraph      状态图
     * @param agentFlowConfig
     * @return 编译图
     * @throws GraphStateException 图状态异常
     */
    private CompiledGraph compiledGraph(StateGraph stateGraph, AgentFlowConfig agentFlowConfig) throws GraphStateException {
        Map<String, Node> nodeMap = agentFlowConfig.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));
        return stateGraph.compile(CompileConfig.builder()
                .withLifecycleListener(new GraphLifecycleListener() {
                    /** 工作流生命周期 **/
                    @Override
                    public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
                        log.info("AgentFlow Lifecycle => status: {}, nodeId: {}, config: {}", FlowStatusEnum.STARTED, nodeId, config);
                        if (config != null && config.getMetadata() != null) {
                            Long instanceId = (Long) config.getMetadata().get("instanceId");
                            Long configId = (Long) config.getMetadata().get("configId");
                            String prompt = (String) config.getMetadata().get("prompt");
                            if (instanceId != null && configId != null) {
                                // 持久化：创建工作流实例
                                agentFlowInstanceRepository.saveFlowInstance(AgentFlowInstance.builder()
                                        .id(instanceId)
                                        .agentConfigId(configId)
                                        .startTime(LocalDateTime.now())
                                        .inputData(JSONUtil.toJsonStr(Map.of("messages", prompt)))
                                        .status(FlowStatusEnum.RUNNING.name())
                                        .build());
                            }
                        }
                        GraphLifecycleListener.super.onStart(nodeId, state, config);
                    }

                    @Override
                    public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
                        log.info("AgentFlow Lifecycle => status: {}, nodeId: {}, config: {}", FlowStatusEnum.SUCCESS, nodeId, config);

                        // 持久化：更新工作流实例状态为 COMPLETED
                        if (config != null && config.getMetadata() != null) {
                            Long instanceId = (Long) config.getMetadata().get("instanceId");
                            Long configId = (Long) config.getMetadata().get("configId");
                            if (instanceId != null && configId != null) {
                                // 持久化：创建工作流实例
                                agentFlowInstanceRepository.updateFlowInstance(AgentFlowInstance.builder()
                                        .id(instanceId)
                                        .endTime(LocalDateTime.now())
                                        .status(FlowStatusEnum.SUCCESS.name())
                                        .build());
                            }
                        }

                        GraphLifecycleListener.super.onComplete(nodeId, state, config);
                    }

                    @Override
                    public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
                        log.error("AgentFlow Lifecycle => status: {}, nodeId: {}, config: {}, exception: {}",
                                FlowStatusEnum.FAILED, nodeId, config, ex.getMessage(), ex);

                        // 持久化：更新工作流实例状态为 FAILED
                        if (config != null && config.getMetadata() != null) {
                            Long instanceId = (Long) config.getMetadata().get("instanceId");
                            Long configId = (Long) config.getMetadata().get("configId");
                            if (instanceId != null && configId != null) {
                                // 持久化：创建工作流实例
                                agentFlowInstanceRepository.updateFlowInstance(AgentFlowInstance.builder()
                                        .id(instanceId)
                                        .endTime(LocalDateTime.now())
                                        .status(FlowStatusEnum.FAILED.name())
                                        .build());
                            }
                        }

                        GraphLifecycleListener.super.onError(nodeId, state, ex, config);
                    }

                    /** 节点生命周期 **/
                    @Override
                    public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
                        log.info("Node Lifecycle => status: {}, nodeId: {}, config: {}, curTime: {}",
                                NodeStatusEnum.RUNNING, nodeId, config, curTime);

                        // 持久化：创建节点实例
                        if (config != null && config.getMetadata() != null) {
                            Long instanceId = (Long) config.getMetadata().get("instanceId");

                            // 创建节点实例记录
                            Node node = nodeMap.get(nodeId);
                            AgentFlowNodeInstance nodeInstance = new AgentFlowNodeInstance();
                            nodeInstance.setAgentInstanceId(instanceId);
                            nodeInstance.setNodeId(nodeId);
                            nodeInstance.setNodeType(node.getType().name());
                            nodeInstance.setStatus(NodeStatusEnum.RUNNING.name());
                            nodeInstance.setStartTime(LocalDateTime.now());
                            nodeInstance.setCreateTime(LocalDateTime.now());

                            agentFlowInstanceRepository.saveNodeInstance(nodeInstance);
                        }

                        GraphLifecycleListener.super.before(nodeId, state, config, curTime);
                    }

                    @Override
                    public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
                        log.info("Node Lifecycle => status: {}, nodeId: {}, config: {}, curTime: {}",
                                NodeStatusEnum.SUCCESS, nodeId, config, curTime);

                        // 持久化：更新节点实例状态
                        if (config != null && config.getMetadata() != null) {
                            Long instanceId = (Long) config.getMetadata().get("instanceId");

                            AgentFlowNodeInstance nodeInstance = agentFlowInstanceRepository.getNodeInstance(instanceId, nodeId);
                            if (nodeInstance != null) {
                                nodeInstance.setStatus(NodeStatusEnum.SUCCESS.name());
                                nodeInstance.setEndTime(LocalDateTime.now());

                                try {
                                    nodeInstance.setOutputData(objectMapper.writeValueAsString(state));
                                } catch (Exception e) {
                                    log.error("序列化节点输出数据失败", e);
                                }

                                agentFlowInstanceRepository.updateNodeInstance(nodeInstance);
                            }
                        }

                        GraphLifecycleListener.super.after(nodeId, state, config, curTime);
                    }
                })
                .build());
    }

}
