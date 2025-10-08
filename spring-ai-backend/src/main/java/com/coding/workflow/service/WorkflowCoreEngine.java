package com.coding.workflow.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.hutool.core.collection.CollectionUtil;
import com.coding.admin.utils.UserContextHolder;
import com.coding.admin.model.model.WorkflowConfigModel;
import com.coding.admin.model.model.WorkflowInstanceModel;
import com.coding.admin.repository.WorkflowInstanceRepository;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.WorkflowStatusEnum;
import com.coding.workflow.model.workflow.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import static com.coding.workflow.enums.NodeTypeEnum.WORKFLOW_FINISH;
import static com.coding.workflow.enums.WorkflowStatusEnum.*;

/**
 * 工作流核心执行引擎 - 最简版本
 * 提取工作流执行的核心链路逻辑
 */
@Slf4j
@Component
public class WorkflowCoreEngine {
    // 最大执行时间
    private static final long MAX_WORKFLOW_EXECUTE_TIMEOUT = 5 * 60 * 1000;
    // 核心组件
    private final Map<String, ExecuteProcessor> processorMap = new HashMap<>();
    // 任务调度线程池
    private final ExecutorService taskExecutorService = Executors.newCachedThreadPool();
    // 节点执行线程池
    private final ExecutorService nodeExecutorService = Executors.newFixedThreadPool(10);

    @Resource
    private WorkflowInstanceRepository workflowInstanceRepository;

    /**
     * 工作流执行入口
     */
    public Long executeWorkflow(WorkflowConfig config, WorkflowContext context) {
        // 1. 初始化上下文
        context.setInstanceId(Instant.now().toEpochMilli());
        context.setWorkflowConfig(config);
        context.setStartTime(LocalDateTime.now());
        context.setTaskStatus(EXECUTING.getCode());

        // 2. 异步执行工作流
        taskExecutorService.submit(() -> {
            try {
                syncExecuteWorkflow(config, context);
            } catch (Exception e) {
                context.setTaskStatus(FAIL.getCode());
                context.setErrorInfo(e.getMessage());
            }
        });

        return context.getInstanceId();
    }

    /**
     * 工作流执行入口，记录到库表
     */
    public Long executeWorkflow(WorkflowConfigModel configModel, Map<String, Object> inputParams) {
        // [Workflow Instance] => Initialize
        Long instanceId = workflowInstanceRepository.add(buildWorkflowInstance(configModel));
        WorkflowConfig config = convertConfigModelToConfig(configModel);
        // 1. 初始化上下文
        WorkflowContext context = new WorkflowContext();
        context.setAppId(configModel.getAppId());
        context.setConfigId(configModel.getId());
        context.setInstanceId(instanceId);
        context.setWorkflowConfig(config);
        context.setStartTime(LocalDateTime.now());
        context.setTaskStatus(EXECUTING.getCode());
        context.getUserMap().putAll(inputParams);

        // 2. 异步执行工作流
        taskExecutorService.submit(() -> {
            try {
                syncExecuteWorkflow(config, context);
            } catch (Exception e) {
                context.setTaskStatus(FAIL.getCode());
                context.setErrorInfo(e.getMessage());
            }
            // [Workflow Instance] => Update Running Status
            workflowInstanceRepository.update(WorkflowInstanceModel.builder()
                    .id(context.getInstanceId())
                    .status(context.getTaskStatus())
                    .message(context.getErrorInfo())
                    .endTime(LocalDateTime.now())
                    .build());
        });

        return context.getInstanceId();
    }

    /**
     * 同步执行工作流 - 核心方法
     */
    private void syncExecuteWorkflow(WorkflowConfig config, WorkflowContext context) throws InterruptedException {
        // 1. 构建DAG图
        DirectedAcyclicGraph<String, Edge> graph = buildDAG(config);

        // 2. 初始化队列和集合
        BlockingQueue<String> taskQueue = new LinkedBlockingQueue<>();
        Set<String> scheduledNodes = new HashSet<>();
        BlockingQueue<String> monitorQueue = new LinkedBlockingQueue<>();

        // 3. 启动监控线程
        taskExecutorService.submit(() -> {
            executeMonitorThread(graph, taskQueue, scheduledNodes, context, monitorQueue);
        });

        // 4. 主执行循环
        AtomicBoolean shouldStop = new AtomicBoolean(false);
        while (!shouldStop.get()) {
            String nodeId = taskQueue.poll(10, TimeUnit.SECONDS);

            if (nodeId != null && !WORKFLOW_FINISH.getCode().equals(nodeId)) {
                // 提交节点执行任务
                nodeExecutorService.submit(() -> {
                    try {
                        executeNode(graph, nodeId, context);
                        monitorQueue.add(NodeStatusEnum.SUCCESS.getCode());
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } else if (WORKFLOW_FINISH.getCode().equals(nodeId)) {
                break;
            }

            shouldStop.set(checkStopCondition(graph, context));
        }

        // 5. 设置最终状态
        if (EXECUTING.getCode().equals(context.getTaskStatus())) {
            context.setTaskStatus(SUCCESS.getCode());
        }
    }

    /**
     * 监控线程：发现可执行节点
     */
    private void executeMonitorThread(DirectedAcyclicGraph<String, Edge> graph,
                                      BlockingQueue<String> taskQueue,
                                      Set<String> scheduledNodes,
                                      WorkflowContext context,
                                      BlockingQueue<String> monitorQueue) {
        AtomicBoolean done = new AtomicBoolean(false);

        while (!done.get()) {
            // 遍历所有节点，查找可执行的节点
            for (String nodeId : graph.vertexSet()) {
                if (canNodeExecute(graph, nodeId, context) &&
                        !scheduledNodes.contains(nodeId)) {
                    taskQueue.add(nodeId);
                    scheduledNodes.add(nodeId);
                }
            }

            done.set(checkStopCondition(graph, context));

            if (!done.get()) {
                try {
                    monitorQueue.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 发送结束信号
        taskQueue.add(WORKFLOW_FINISH.getCode());
    }

    /**
     * 判断节点是否可执行
     */
    private boolean canNodeExecute(DirectedAcyclicGraph<String, Edge> graph, String nodeId, WorkflowContext context) {
        // 如果节点已有结果，不能重复执行
        if (context.getNodeResultMap().containsKey(nodeId)) {
            return false;
        }

        // 获取前置节点
        Set<Edge> incomingEdges = graph.incomingEdgesOf(nodeId);
        if (incomingEdges.isEmpty()) {
            return true; // 起始节点可直接执行
        }

        // 检查所有前置节点是否已完成
        for (Edge ancestor : incomingEdges) {
            NodeResult result = context.getNodeResultMap().get(ancestor.getSource());
            if (result == null) {
                return false;
            }

            String status = result.getNodeStatus();
            if (NodeStatusEnum.FAIL.getCode().equals(status)
                    || NodeStatusEnum.EXECUTING.getCode().equals(status)
                    || NodeStatusEnum.PAUSE.getCode().equals(status)
                    || NodeStatusEnum.STOP.getCode().equals(status)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 执行单个节点
     * 判断前驱节点是否执行完毕：
     * 如果前驱节点是条件分支节点，则检查其MultiBranchReference，判断当前节点是否跳过（只有nodeId在条件分支的targetIds中才执行，否则跳过）
     */
    private void executeNode(DirectedAcyclicGraph<String, Edge> graph, String nodeId, WorkflowContext context) {
        try {
            // 加锁防止并发执行
            context.getLock().lock();
            NodeResult nodeResult = new NodeResult();
            try {
                // 再次检查，防止重复执行
                if (context.getNodeResultMap().containsKey(nodeId)) {
                    return; // 已被其他线程执行
                }
                nodeResult.setNodeId(nodeId);
                nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
                nodeResult.setNodeStartTime(LocalDateTime.now());
                context.getNodeResultMap().put(nodeId, nodeResult);
            } finally {
                context.getLock().unlock();
            }

            // 检查前驱条件分支节点的状态，MultiBranchReference标记了执行哪个分支
            // 条件边时，仅执行一条分支，跳过其余分支
            Set<Edge> incomingEdges = graph.incomingEdgesOf(nodeId);
            if (!incomingEdges.isEmpty()) {

                boolean skipResult = incomingEdges.stream().anyMatch(incomingEdge -> {
                    NodeResult result = context.getNodeResultMap().get(incomingEdge.getSource());

                    if (result.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode()) && result.isMultiBranch()) {
                        // 还没执行过
                        List<NodeResult.MultiBranchReference> multiBranchResults = result.getMultiBranchResults();
                        if (CollectionUtil.isEmpty(multiBranchResults)) {
                            return true;
                        } else {
                            Optional<NodeResult.MultiBranchReference> referenceOptional = multiBranchResults.stream()
                                    .filter(branchResult -> branchResult.getTargetIds().contains(nodeId))
                                    .findFirst();
                            if (!referenceOptional.isPresent()) {
                                return true;
                            }
                        }
                        return false;
                    }
                    return result.getNodeStatus().equals(NodeStatusEnum.SKIP.getCode());
                });
                if (skipResult) {
                    log.info("跳过节点: {} 的执行，并标记为Skip状态", nodeId);
                    nodeResult.setNodeStatus(NodeStatusEnum.SKIP.getCode());
                    nodeResult.setNodeId(nodeId);
                    context.getNodeResultMap().put(nodeId, nodeResult);
                    return;
                }
            }

            // 获取节点配置
            Node node = findNodeById(context.getWorkflowConfig(), nodeId);
            if (node == null) {
                throw new RuntimeException("节点不存在: " + nodeId);
            }

            // 执行节点逻辑
            ExecuteProcessor processor = processorMap.get(node.getType());
            processor.execute(graph, node, context);
        } catch (Exception e) {
            NodeResult nodeResult = new NodeResult();
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            nodeResult.setNodeExecuteTime(String.valueOf(LocalDateTime.now()));
            nodeResult.setErrorInfo(e.getMessage());
            context.setTaskStatus(FAIL.getCode());

            log.error("节点执行失败: {}, 错误: {}", nodeId, e.getMessage());
        }
    }

    /**
     * 构建DAG图
     */
    private DirectedAcyclicGraph<String, Edge> buildDAG(WorkflowConfig config) {
        DirectedAcyclicGraph<String, Edge> graph = new DirectedAcyclicGraph<>(null, Edge::new, false);

        // 添加节点
        for (Node node : config.getNodes()) {
            graph.addVertex(node.getId());
        }

        // 添加边
        for (Edge edge : config.getEdges()) {
            graph.addEdge(edge.getSource(), edge.getTarget(), edge);
        }

        // TODO 检查联通性
        // TODO 检查所有节点是否在边中出现

        return graph;
    }

    /**
     * 检查停止条件
     */
    private boolean checkStopCondition(DirectedAcyclicGraph<String, Edge> graph, WorkflowContext context) {
        // 手动停止
        if (STOP.getCode().equals(context.getTaskStatus()) || FAIL.getCode().equals(context.getTaskStatus())) {
            return true;
        }

        // 超时检查
        long runTime = Duration.between(context.getStartTime(), LocalDateTime.now()).toMillis();
        if (runTime > MAX_WORKFLOW_EXECUTE_TIMEOUT) { // 5分钟超时
            context.setTaskStatus(TIMEOUT.getCode());
            return true;
        }

        // 检查是否有结束节点完成
        for (String nodeId : graph.vertexSet()) {
            // 通过节点ID找到对应的节点配置
            Node node = findNodeById(context.getWorkflowConfig(), nodeId);
            if (node != null && NodeTypeEnum.END.getCode().equals(node.getType())) {
                NodeResult result = context.getNodeResultMap().get(nodeId);
                if (result != null && NodeStatusEnum.SUCCESS.getCode().equals(result.getNodeStatus())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 根据ID查找节点
     */
    private Node findNodeById(WorkflowConfig config, String nodeId) {
        return config.getNodes().stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 注册节点处理器
     */
    public void registerProcessor(String nodeType, ExecuteProcessor processor) {
        processorMap.put(nodeType, processor);
    }

    // ==================== 辅助方法 ====================
    private WorkflowConfig convertConfigModelToConfig(WorkflowConfigModel config) {
        WorkflowConfig workflowConfig = new WorkflowConfig();
        workflowConfig.setNodes(config.getNodes());
        workflowConfig.setEdges(config.getEdges());
        return workflowConfig;
    }

    private WorkflowInstanceModel buildWorkflowInstance(WorkflowConfigModel configModel) {
        LocalDateTime now = LocalDateTime.now();
        return WorkflowInstanceModel.builder()
                .workflowConfigId(configModel.getId())
                .appId(configModel.getAppId())
                .version(configModel.getVersion())
                .status(WorkflowStatusEnum.EXECUTING.getCode())
                .startTime(now)
                .creator(UserContextHolder.getUserId())
                .build();
    }

}
