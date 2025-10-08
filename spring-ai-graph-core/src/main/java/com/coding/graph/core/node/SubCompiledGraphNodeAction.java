package com.coding.graph.core.node;

import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public record SubCompiledGraphNodeAction(String nodeId, CompileConfig parentCompileConfig,
                                         CompiledGraph subGraph) implements AsyncNodeActionWithConfig {
    public String subGraphId() {
        return format("subgraph_%s", nodeId);
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {

        final CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        try {
            var generator = subGraph.streamFromInitialNode(state, config);

            future.complete(Map.of(format("%s_%s", subGraphId(), UUID.randomUUID()), generator));

        }
        catch (Exception e) {

            future.completeExceptionally(e);
        }

        return future;
    }
}
