package com.coding.graph.core.graph;

import com.coding.graph.core.edge.Edge;
import com.coding.graph.core.edge.EdgeCondition;
import com.coding.graph.core.edge.EdgeValue;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.node.Node;
import com.coding.graph.core.node.SubStateGraphNode;
import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.node.command.AsyncCommandAction;
import com.coding.graph.core.node.action.AsyncNodeAction;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

import static com.coding.graph.core.common.NodeCodeConstants.END;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StateGraph {

    private String name;

    // 构建OverAllState中Key的更新策略工厂
    private KeyStrategyFactory keyStrategyFactory;

    private final Nodes nodes = new Nodes();

    private final Edges edges = new Edges();

    // 添加节点的方法
    public StateGraph addNode(String id, Node node) throws GraphStateException {
        if (Objects.equals(node.getId(), END)) {
            throw new GraphStateException("节点ID不能为结束节点");
        }
        if (!Objects.equals(node.getId(), id)) {
            throw new GraphStateException("节点ID不匹配: " + id + " != " + node.getId());
        }

        if (nodes.elements.contains(node)) {
            throw new GraphStateException("节点已存在: " + id);
        }

        nodes.elements.add(node);
        return this;
    }

    public StateGraph addNode(String id, AsyncNodeAction action) throws GraphStateException {
        return addNode(id, AsyncNodeActionWithConfig.of(action));
    }

    public StateGraph addNode(String id, AsyncNodeActionWithConfig actionWithConfig) throws GraphStateException {
        Node node = new Node(id, (config) -> actionWithConfig);
        return addNode(id, node);
    }

    public StateGraph addNode(String id, StateGraph subGraph) throws  GraphStateException {
        if (Objects.equals(id, END)) {
            throw new GraphStateException("节点ID不能为结束节点");
        }

        // TODO 验证子图

        var node = new SubStateGraphNode(id, subGraph);

        if (nodes.elements.contains(node)) {
            throw new GraphStateException("节点已存在: " + id);
        }
        nodes.elements.add(node);
        return this;
    }

    // 添加边的方法
    public StateGraph addEdge(String sourceId, String targetId) throws GraphStateException {
        if (Objects.equals(sourceId, END)) {
            throw new GraphStateException("结束节点不能作为起始节点");
        }

        var newEdge = new Edge(sourceId, new EdgeValue(targetId));

        // 合并同一起点的边的目标节点
        int index = edges.elements.indexOf(newEdge);
        if (index >= 0) {
            var newTargets = new ArrayList<>(edges.elements.get(index).targets());
            newTargets.add(newEdge.target());
            edges.elements.set(index, new Edge(sourceId, newTargets));
        }
        else {
            edges.elements.add(newEdge);
        }

        return this;
    }

    public StateGraph addConditionalEdges(String sourceId, AsyncCommandAction condition, Map<String, String> mappings) throws GraphStateException {
        if (Objects.equals(sourceId, END)) {
            throw new GraphStateException("结束节点不能作为起始节点");
        }
        if (mappings == null || mappings.isEmpty()) {
            throw new GraphStateException("条件边的映射不能为空");
        }

        var newEdge = new Edge(sourceId, new EdgeValue(new EdgeCondition(condition, mappings)));

        if (edges.elements.contains(newEdge)) {
            throw new GraphStateException("条件边已存在: " + sourceId);
        }
        else {
            edges.elements.add(newEdge);
        }
        return this;
    }

    public StateGraph addConditionalEdges(String sourceId, AsyncEdgeAction condition, Map<String, String> mappings)
            throws GraphStateException {
        return addConditionalEdges(sourceId, AsyncCommandAction.of(condition), mappings);
    }

    // 编译图的方法
    public CompiledGraph compile(CompileConfig config) throws GraphStateException {
        Objects.requireNonNull(config, "配置不能为空");

        // TODO 验证图 基本检查：1.节点正确性 2.边的起点和终点正确性 3.入口和"出口❌"节点存在性 4.无孤立节点? 5.无环路?

        return new CompiledGraph(this, config);
    }

    public CompiledGraph compile() throws GraphStateException {
        return compile(new CompileConfig());
    }

    @Getter
    public static class Nodes {
        public final Set<Node> elements;
        // 构造函数
        public Nodes() {
            this.elements = new LinkedHashSet<>();
        }
        public Nodes(Collection<Node> elements) {
            this.elements = new LinkedHashSet<>(elements);
        }
        // 获取节点的方法
        public boolean anyMatchById(String id) {
            return elements.stream().anyMatch(n -> Objects.equals(n.getId(), id));
        }
        // 获取所有为子图的节点
        public List<SubStateGraphNode> onlySubStateGraphNodes() {
            return elements.stream()
                    .filter(n -> n instanceof SubStateGraphNode)
                    .map(n -> (SubStateGraphNode) n)
                    .toList();
        }
        // 获取所有非子图的节点
        public Collection<Node> exceptSubStateGraphNodes() {
            return elements.stream()
                    .filter(n -> !(n instanceof SubStateGraphNode))
                    .toList();
        }
    }

    @Getter
    public static class Edges {
        public final List<Edge> elements;
        // 构造函数
        public Edges() {
            this.elements = new LinkedList<>();
        }
        public Edges(Collection<Edge> elements) {
            this.elements = new LinkedList<>(elements);
        }
        // 获取边的方法
        public Optional<Edge> edgeBySourceId(String sourceId) {
            return elements.stream().filter(e -> Objects.equals(e.sourceId(), sourceId)).findFirst();
        }
        public List<Edge> edgesByTargetId(String targetId) {
            return elements.stream().filter(e -> e.anyMatchByTargetId(targetId)).toList();
        }
    }
}
