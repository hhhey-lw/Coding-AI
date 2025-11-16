package com.coding.graph.core.node;

import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.graph.SubGraphNode;

public class SubStateGraphNode extends Node implements SubGraphNode {

    // 子图
    private final StateGraph subGraph;

    public SubStateGraphNode(String id, StateGraph subGraph) {
        super(id);
        this.subGraph = subGraph;
    }

    @Override
    public StateGraph subGraph() {
        return subGraph;
    }
}
