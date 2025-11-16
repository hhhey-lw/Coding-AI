package com.coding.graph.core.node;

import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.graph.SubGraphNode;

import java.util.Objects;

public class SubCompiledGraphNode extends Node implements SubGraphNode {

    private final CompiledGraph subGraph;

    public SubCompiledGraphNode(String id, CompiledGraph subGraph) {
        super(Objects.requireNonNull(id, "Id不能为空"),
                (config) -> new SubCompiledGraphNodeAction(id, config, subGraph));
        this.subGraph = subGraph;
    }

    @Override
    public StateGraph subGraph() {
        return null;
    }
}
