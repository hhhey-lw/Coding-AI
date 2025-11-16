package com.coding.graph.core.node.action;

import com.coding.graph.core.state.OverAllState;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 异步边动作接口，定义了一个接受 OverAllState 并返回 CompletableFuture<String> 的方法。
 */
@FunctionalInterface
public interface AsyncEdgeAction extends Function<OverAllState, CompletableFuture<String>> {

    CompletableFuture<String> apply(OverAllState state);

    static AsyncEdgeAction edge_async(EdgeAction syncAction) {
        return state -> {
            CompletableFuture<String> result = new CompletableFuture<>();
            try {
                result.complete(syncAction.apply(state));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }

}
