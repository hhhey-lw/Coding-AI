package com.coding.graph.core.node.action;

import com.coding.graph.core.state.OverAllState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 异步节点行为接口
 * 接收全局状态模型，返回一个异步的处理结果
 */
public interface AsyncNodeAction extends Function<OverAllState, CompletableFuture<Map<String, Object>>> {

    CompletableFuture<Map<String, Object>> apply(OverAllState state);

    static AsyncNodeAction node_async(NodeAction syncAction) {
        return (state) -> {
            CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
            try {
                result.complete(syncAction.apply(state));
            }
            catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }
}
