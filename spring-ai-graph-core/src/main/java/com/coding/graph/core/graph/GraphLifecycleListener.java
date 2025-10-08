package com.coding.graph.core.graph;

import com.coding.graph.core.node.config.RunnableConfig;

import java.util.Map;

public interface GraphLifecycleListener {

    default void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
    }

    default void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
    }

    default void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
    }

    default void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
    }

    default void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
    }

}
