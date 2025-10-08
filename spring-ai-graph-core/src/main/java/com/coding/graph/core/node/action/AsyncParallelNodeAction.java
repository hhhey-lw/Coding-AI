package com.coding.graph.core.node.action;

import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * 并行节点动作：并行执行多个子节点动作，并合并结果。
 */
public record AsyncParallelNodeAction(String nodeId, List<AsyncNodeActionWithConfig> actions, CompileConfig compileConfig)
        implements AsyncNodeActionWithConfig {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Function<AsyncNodeActionWithConfig, CompletableFuture<Map<String, Object>>> evalNodeAction = config.metadata(nodeId)
                .filter(value -> value instanceof Executor)
                .map(Executor.class::cast)
                .map(executor -> (Function<AsyncNodeActionWithConfig, CompletableFuture<Map<String, Object>>>) action -> evalNodeActionAsync(
                        action, state, config, executor))
                .orElseGet(() -> action -> evalNodeActionSync(action, state, config));

        // 并行执行所有 action
        List<CompletableFuture<Map<String, Object>>> futures = actions.stream().map(evalNodeAction).toList();

        // 等待所有 action 完成并收集结果
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
            // 收集所有结果
            List<Map<String, Object>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // 合并结果
            return results.stream()
                    .reduce(state.data(), (acc, partial) -> OverAllState.updateState(acc, partial, state.keyStrategies()));
        });
    }

    private CompletableFuture<Map<String, Object>> evalNodeActionAsync(AsyncNodeActionWithConfig action,
                                                                       OverAllState state, RunnableConfig config, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return evalNodeActionSync(action, state, config).join();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private CompletableFuture<Map<String, Object>> evalNodeActionSync(AsyncNodeActionWithConfig action,
                                                                      OverAllState state, RunnableConfig config) {
        return action.apply(state, config);
    }

}
