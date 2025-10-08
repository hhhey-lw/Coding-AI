package com.coding.graph.core.node.action;

import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * 异步节点行为接口，带有可运行配置
 * 接收全局状态模型和可运行配置，返回一个异步的处理结果
 */
public interface AsyncNodeActionWithConfig
        extends BiFunction<OverAllState, RunnableConfig, CompletableFuture<Map<String, Object>>> {

    CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config);

    static AsyncNodeActionWithConfig node_async(NodeActionWithConfig syncAction) {
        return (state, config) -> {
            CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
            try {
                result.complete(syncAction.apply(state, config));
            }
            catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }

    /**
     * 将一个简单的 AsyncNodeAction 适配为 AsyncNodeActionWithConfig。
     * @param action 被适配的简单 AsyncNodeAction
     * @return 一个包装了给定 AsyncNodeAction 的 AsyncNodeActionWithConfig
     */
    static AsyncNodeActionWithConfig of(AsyncNodeAction action) {
        return (state, config) -> action.apply(state);
    }

}
