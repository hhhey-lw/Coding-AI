package com.coding.graph.core.node;

import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.action.AsyncParallelNodeAction;
import com.coding.graph.core.node.config.CompileConfig;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * 并行节点：聚合起点相同但终点不同的边
 */
public class ParallelNode extends Node{

    public static final String PARALLEL_PREFIX = "__PARALLEL__";

    public static String formatNodeId(String nodeId) {
        return format("%s(%s)", PARALLEL_PREFIX, requireNonNull(nodeId, "nodeId 不能为 null!"));
    }

    public ParallelNode(String id, List<AsyncNodeActionWithConfig> actions, CompileConfig compileConfig) {
        super(formatNodeId(id),
                (config) -> new AsyncParallelNodeAction(formatNodeId(id), actions, compileConfig));
    }

    @Override
    public final boolean isParallel() {
        return true;
    }

}
