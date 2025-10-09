package com.coding.graph.core.graph;

import com.coding.graph.core.edge.Edge;
import com.coding.graph.core.edge.EdgeValue;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.generator.AsyncNodeGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.ParallelNode;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.command.Command;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.graph.StateGraph.Nodes;
import com.coding.graph.core.graph.StateGraph.Edges;
import com.coding.graph.core.state.strategy.KeyStrategy;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;

@Getter
public class CompiledGraph {

    // 原始图和编译配置
    private final StateGraph stateGraph;
    private final Map<String, KeyStrategy> keyStrategyMap;
    private final CompileConfig compileConfig;
    private final ProcessedNodesEdgesAndConfig processedData;

    // 节点和边：node<nodeId, nodeAction> && edge<edgeId, targetEdgeInfo>
    final Map<String, AsyncNodeActionWithConfig> nodes = new LinkedHashMap<>();
    final Map<String, EdgeValue> edges = new LinkedHashMap<>();

    // 构造函数
    public CompiledGraph(StateGraph stateGraph, CompileConfig compileConfig) throws GraphStateException {
        this.stateGraph = stateGraph;
        // 设置Key的更新策略
        this.keyStrategyMap = stateGraph.getKeyStrategyFactory()
                .apply()
                .entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.compileConfig = compileConfig;

        // 预处理图数据
        this.processedData = ProcessedNodesEdgesAndConfig.process(stateGraph);

        // 编译节点
        for (var node : processedData.nodes.getElements()) {
            var factory = node.getActionFactory();
            Objects.requireNonNull(factory, "节点的ActionFactory不能为空: " + node.getId());
            nodes.put(node.getId(), factory.apply(compileConfig));
        }

        // 编译边
        for (var edge : processedData.edges.getElements()) {
            var targets = edge.targets();
            // 情况1：单目标边 - 直接连接源节点到目标节点
            if (targets.size() == 1) {
                edges.put(edge.sourceId(), targets.get(0));
            }
            // 情况2：多目标边 - 需要创建并行节点来处理多个目标
            else {
                // 并行边，过滤掉不存在的目标节点
                Supplier<Stream<EdgeValue>> parallelNodeStream = () -> targets.stream()
                        .filter(target -> nodes.containsKey(target.id()));

                /*    -> B ->
                 *  A -> C -> E   仅支持这种形式的并行节点
                 *    -> D ->
                 */
                var parallelNodeEdges = parallelNodeStream.get()
                        .map(target -> new Edge(target.id()))
                        .filter(ee -> stateGraph.getEdges().elements.contains(ee))
                        .map(ee -> stateGraph.getEdges().elements.indexOf(ee))
                        .map(index -> stateGraph.getEdges().elements.get(index))
                        .toList();

                // 收集所有并行节点边的目标节点ID
                var parallelNodeTargets = parallelNodeEdges.stream()
                        .map(e -> e.target().id())
                        .collect(Collectors.toSet());

                // 验证并行节点的合法性：不能有多个不同的目标节点
                if (parallelNodeTargets.size() > 1) {

                    var conditionalEdges = parallelNodeEdges.stream()
                            .filter(ee -> ee.target().value() != null)
                            .toList();

                    if (!conditionalEdges.isEmpty()) {
                        throw new GraphStateException("并行边不能包含条件边: " + edge.sourceId());
                    }

                    throw new GraphStateException("并行边不能有多个不同的目标节点: " + edge.sourceId() + " -> " + parallelNodeTargets);
                }

                // 获取并行执行的动作列表
                var actions = parallelNodeStream.get()
                        .map(target -> nodes.get(target.id()))
                        .toList();


                // 创建并行节点，用于管理多个并行执行的动作
                var parallelNode = new ParallelNode(edge.sourceId(), actions, compileConfig);

                // 将并行节点添加到节点列表中
                nodes.put(parallelNode.getId(), parallelNode.getActionFactory().apply(compileConfig));
                // 连接源节点到并行节点，再连接并行节点到实际的目标节点
                edges.put(edge.sourceId(), new EdgeValue(parallelNode.getId()));
                edges.put(parallelNode.getId(), new EdgeValue(parallelNodeTargets.iterator().next()));
            }
        }
    }

    /**
     * 根据提供的输入参数，创建一个异步生成器（AsyncGenerator）流，用于输出 NodeOutput 类型的数据。
     * 注意这里仅返回generator，运行需要手动调用 => forEachAsync 触发节点级别的迭代
     */
    public AsyncGenerator<NodeOutput> stream(Map<String, Object> inputs) throws GraphRunnerException {
        OverAllState overAllState = new OverAllState(new HashMap<>(inputs), keyStrategyMap);
        return this.streamFromInitialNode(overAllState, RunnableConfig.builder().build());
    }

    // 执行函数
    public Optional<OverAllState> invoke(Map<String, Object> inputs, RunnableConfig config) {
        return streamFromInitialNode(OverAllState.builder()
                .data(new HashMap<>(inputs))
                .keyStrategies(keyStrategyMap)
                .build(), config)
                .stream()
                .reduce((a, b) -> b)
                .map(NodeOutput::getState);
    }

    public Optional<OverAllState> invoke(Map<String, Object> inputs) {
        return this.invoke(inputs, RunnableConfig.builder().build());
    }

    // 辅助函数
    public AsyncGenerator<NodeOutput> streamFromInitialNode(OverAllState overAllState, RunnableConfig config) {
        Objects.requireNonNull(config, "运行配置不能为空");
        final AsyncNodeGenerator<NodeOutput> generator = new AsyncNodeGenerator<>(this, config, overAllState);
        // WithResult 调整为 WithEmbed
        return new AsyncGenerator.WithEmbed<>(generator);
    }

    /**
     * 获取入口节点
     * @param state 节点入参
     * @param config 运行时配置
     * @return 入口节点的命令: 包括目标节点和更新的参数状态
     */
    public Command getEntryPoint(Map<String, Object> state, RunnableConfig config) throws Exception {
        var entryPoint = this.edges.get(START);
        return nextNodeId(entryPoint, state, "entryPoint", config);
    }

    public Command nextNodeId(String nodeId, Map<String, Object> state, RunnableConfig config) throws Exception {
        return nextNodeId(edges.get(nodeId), state, nodeId, config);
    }

    public Command nextNodeId(EdgeValue route, Map<String, Object> state, String nodeId, RunnableConfig config)
            throws Exception {

        if (route == null) {
            throw new GraphRunnerException("找不到节点{" + nodeId + "}的边" );
        }
        if (route.id() != null) {
            return new Command(route.id(), state);
        }
        if (route.value() != null) {
            OverAllState derefState = new OverAllState(state);

            var command = route.value().action().apply(derefState, config).get();

            var newRoute = command.gotoNode();

            String result = route.value().mappings().get(newRoute);
            if (result == null) {
                throw new GraphRunnerException("找不到节点{" + nodeId + "}的条件边映射: " + newRoute);
            }

            var currentState = OverAllState.updateState(state, command.update(), this.keyStrategyMap);

            return new Command(result, currentState);
        }
        throw new GraphRunnerException("节点{" + nodeId + "}的边配置不正确");
    }

    /**
     * 存储全部的节点和边
     */
    record ProcessedNodesEdgesAndConfig(Nodes nodes, Edges edges) {
        /**
         * 预处理图，展开子图节点
         */
        static ProcessedNodesEdgesAndConfig process(StateGraph stateGraph) throws GraphStateException {
            var subgraphNodes = stateGraph.getNodes().onlySubStateGraphNodes();
            // 没有子图节点，返回全部的普通节点和边
            if (subgraphNodes.isEmpty()) {
                return new ProcessedNodesEdgesAndConfig(stateGraph.getNodes(), stateGraph.getEdges());
            }

            var nodes = new Nodes(stateGraph.getNodes().exceptSubStateGraphNodes());
            var edges = new Edges(stateGraph.getEdges().elements);
            // 将子图节点展开为 普通节点和边
            for (var subgraphNode : subgraphNodes) {
                // 递归展平嵌套的子图
                var sgWorkflow = subgraphNode.subGraph();
                ProcessedNodesEdgesAndConfig processedSubGraph = process(sgWorkflow);

                Nodes processedSubGraphNodes = processedSubGraph.nodes;
                Edges processedSubGraphEdges = processedSubGraph.edges;

                // 处理 START 节点
                Edge sgEdgeStart = processedSubGraphEdges.edgeBySourceId(START).orElseThrow();
                if (sgEdgeStart.isParallel()) {
                    throw new GraphStateException("子图的起始节点不能为并行边: " + subgraphNode.getId());
                }

                EdgeValue sgEdgeStartTarget = sgEdgeStart.target();
                if (sgEdgeStartTarget == null) {
                    throw new GraphStateException("子图的起始节点没有目标节点: " + subgraphNode.getId());
                }
                // 找到所有以子图节点为目标节点的边，更新它们的目标节点为子图的起始节点的目标节点
                List<Edge> edgesWithSubgraphTargetId = edges.edgesByTargetId(subgraphNode.getId());
                if (edgesWithSubgraphTargetId.isEmpty()) {
                    throw new GraphStateException("找不到子图节点: " + subgraphNode.getId() + "的下一个节点");
                }
                // 终点为子图的节点Id的边，更新为子图起始节点的目标Id
                for (Edge edgeWithSubgraphTargetId : edgesWithSubgraphTargetId) {
                    // 构建新的边，功能：跳过START节点，直连START的目标节点
                    Edge newEdge = edgeWithSubgraphTargetId.withSourceAndTargetIdsUpdated(Function.identity(),
                            id -> new EdgeValue((Objects.equals(id, subgraphNode.getId()) // 更新目标节点的Id，追加上子图的Id前缀
                                    ? subgraphNode.formatId(sgEdgeStartTarget.id()) : id)));
                    edges.elements.remove(edgeWithSubgraphTargetId);
                    edges.elements.add(newEdge);
                }
                // 处理 END 节点，将所有以 END 为目标节点的边，更新它们的目标节点为子图节点的出边的目标节点
                var sgEdgesEnd = processedSubGraphEdges.edgesByTargetId(END);

                Edge edgeWithSubgraphSourceId = edges.edgeBySourceId(subgraphNode.getId()).orElseThrow();
                if (edgeWithSubgraphSourceId.isParallel()) {
                    throw new GraphStateException("子图节点的出边不能为并行边: " + subgraphNode.getId());
                }

                sgEdgesEnd.stream()
                        .map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode::formatId,
                                id -> (Objects.equals(id, END) ? edgeWithSubgraphSourceId.target()
                                        : new EdgeValue(subgraphNode.formatId(id)))))
                        .forEach(edges.elements::add);
                edges.elements.remove(edgeWithSubgraphSourceId);

                // 处理边
                processedSubGraphEdges.elements.stream()
                        .filter(e -> !Objects.equals(e.sourceId(), START))
                        .filter(e -> !e.anyMatchByTargetId(END))
                        .map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode::formatId,
                                id -> new EdgeValue(subgraphNode.formatId(id))))
                        .forEach(edges.elements::add);
                // 处理节点
                processedSubGraphNodes.elements.stream()
                        .map(n -> n.withIdUpdated(subgraphNode::formatId))
                        .forEach(nodes.elements::add);
            }
            return new ProcessedNodesEdgesAndConfig(nodes, edges);
        }
    }

}
