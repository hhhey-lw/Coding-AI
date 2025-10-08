package com.coding.graph.core.graph;

public interface SubGraphNode {
    String PREFIX_FORMAT = "%s-%s";

    // 子图ID
    String getId();

    StateGraph subGraph();

    default String formatId(String subGraphNodeId, String nodeId) {
        return String.format(PREFIX_FORMAT, subGraphNodeId, nodeId);
    }

    default String formatId(String nodeId) {
        return String.format(PREFIX_FORMAT, getId(), nodeId);
    }

}
