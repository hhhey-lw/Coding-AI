package com.coding.graph.core.node.command;

import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * 异步命令行为接口
 * 接收全局状态模型和可运行配置，返回一个异步的命令：命令包含目标节点
 */
public interface AsyncCommandAction extends BiFunction<OverAllState, RunnableConfig, CompletableFuture<Command>> {

    static AsyncCommandAction node_async(CommandAction syncAction) {
        return (state, config) -> {
            var result = new CompletableFuture<Command>();
            try {
                result.complete(syncAction.apply(state, config));
            }
            catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }

    static AsyncCommandAction of(AsyncEdgeAction action) {
        return (state, config) -> action.apply(state).thenApply(Command::new);
    }

}
