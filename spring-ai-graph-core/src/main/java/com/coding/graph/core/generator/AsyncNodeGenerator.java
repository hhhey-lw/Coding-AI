package com.coding.graph.core.generator;

import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.GraphLifecycleListener;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.utils.LifeListenerUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.coding.graph.core.common.NodeCodeConstants.*;

/**
 * 异步节点生成器：状态机
 * 负责根据编译图和当前状态迭代执行节点
 *
 * @param <Output> 输出类型
 */
@Slf4j
public class AsyncNodeGenerator<Output> implements AsyncGenerator<Output> {
    // 编译图
    private final CompiledGraph compiledGraph;

    // 节点执行上下文
    private final Context context;
    private int iteration = 0;
    private static final int MAX_ITERATIONS = 25;
    // 运行配置
    private final RunnableConfig config;
    // 当前节点的状态 TODO 确定这个的作用！
    Map<String, Object> currentState;
    // 全局状态
    private final OverAllState overAllState;

    // 构造函数
    public AsyncNodeGenerator(CompiledGraph compiledGraph, RunnableConfig config, OverAllState overAllState) {
        this.compiledGraph = compiledGraph;
        this.config = config;
        this.overAllState = overAllState;
        this.context = new Context();
        Map<String, Object> inputs = overAllState.data();
        this.currentState = OverAllState.updateState(new HashMap<>(), inputs, this.overAllState.keyStrategies());
    }

    // 迭代执行节点
    @Override
    public Data<Output> next() {
        try {
            // 防止无限循环
            if (++iteration > MAX_ITERATIONS) {
                return Data.error(new IllegalStateException("达到最大迭代次数，停止迭代"));
            }

            // 结束节点的迭代
            if (context.nextNodeId() == null && context.currentNodeId() == null) {
                return Data.done(currentState);
            }

            // 开始节点入口
            if (START.equals(context.currentNodeId())) {
                // 设置图的监听器
                doListeners(START, null);
                // 设置下一个节点
                var nextNodeCommand = compiledGraph.getEntryPoint(currentState, config);
                context.setNextNodeId(nextNodeCommand.gotoNode());
                currentState = nextNodeCommand.update();
                // 跳转到下一个节点
                context.setCurrentNodeId(nextNodeCommand.gotoNode());
                return Data.of(CompletableFuture.completedFuture(null));
            }

            // 结束节点出口
            if (END.equals(context.nextNodeId())) {
                context.reset();
                doListeners(END, null);
                return Data.of(buildNodeOutput(END));
            }

            // 正常节点执行
            context.setCurrentNodeId(context.nextNodeId());
            var action = compiledGraph.getNodes().get(context.currentNodeId());
            if (action == null) {
                throw new GraphRunnerException("节点未找到: " + context.currentNodeId());
            }

            return evaluateAction(action, this.overAllState).get();

        } catch (Exception e) {
            doListeners(ERROR, e);
            log.error("编译图运行失败，错误信息：{}", e.getMessage(), e);
            return Data.error(e);
        }
    }

    private CompletableFuture<Data<Output>> evaluateAction(AsyncNodeActionWithConfig action, OverAllState overAllState) {
        doListeners(NODE_BEFORE, null);
        return action.apply(overAllState, config)
                // updateState 是具体节点返回的Map结果
                .thenApply((updateState) -> {
                    try {
                        // FIX: SUPPORT STREAM CHAT
                        Optional<Data<Output>> embed = getEmbedGenerator(updateState);
                        if (embed.isPresent()) {
                            return embed.get();
                        }

                        // 更新状态
                        this.currentState = OverAllState.updateState(currentState, updateState, this.overAllState.keyStrategies());
                        this.overAllState.updateState(updateState);
                        // 计算下一个节点
                        var nextNodeCommand = compiledGraph.nextNodeId(context.currentNodeId(), currentState, config);
                        context.setNextNodeId(nextNodeCommand.gotoNode());
                        currentState = nextNodeCommand.update();
                        return Data.of(buildNodeOutput(context.currentNodeId()));
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .whenComplete((outputData, throwable) -> {
                    doListeners(NODE_AFTER, null);
                });
    }

    private Optional<Data<Output>> getEmbedGenerator(Map<String, Object> partialState) {
        return partialState.entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof AsyncGenerator)
                .findFirst()
                // 迭代器和最终的回调函数
                .map(generatorEntry -> {
                    final var generator = (AsyncGenerator<Output>) generatorEntry.getValue();
                    return Data.composeWith(generator.map(n -> {
                        // n.setSubGraph(true);
                        return n;
                    }), data -> {

                        if (data != null) {

                            if (data instanceof Map<?, ?>) {
                                var partialStateWithoutGenerator = partialState.entrySet()
                                        .stream()
                                        .filter(e -> !Objects.equals(e.getKey(), generatorEntry.getKey()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                                var intermediateState = OverAllState.updateState(currentState,
                                        partialStateWithoutGenerator, this.overAllState.keyStrategies());

                                currentState = OverAllState.updateState(intermediateState, (Map<String, Object>) data,
                                        this.overAllState.keyStrategies());
                                this.overAllState.updateState(currentState);
                            }
                            else {
                                throw new IllegalArgumentException("Embedded generator must return a Map");
                            }
                        }

                        var nextNodeCommand = this.compiledGraph.nextNodeId(context.currentNodeId(), currentState, config);
                        context.setNextNodeId(nextNodeCommand.gotoNode());
                        currentState = nextNodeCommand.update();

                    });
                });
    }

    // 构建节点输出
    @SuppressWarnings("unchecked")
    protected CompletableFuture<Output> buildNodeOutput(String nodeId) {
        OverAllState state = new OverAllState(currentState);
        return CompletableFuture.completedFuture((Output) NodeOutput.of(nodeId, state));
    }

    // 执行监听器
    private void doListeners(String scene, Exception e) {
        Deque<GraphLifecycleListener> listeners = new LinkedBlockingDeque<>(this.compiledGraph.getCompileConfig().lifecycleListeners());
        LifeListenerUtil.processListenersLIFO(this.context.currentNodeId(), listeners, this.currentState,
                this.config, scene, e);
    }

    // 节点执行上下文
    public static class Context {
        private String currentNodeId;
        private String nextNodeId;

        public Context() {
            this.currentNodeId = START;
            this.nextNodeId = null;
        }

        String nextNodeId() {
            return nextNodeId;
        }
        void setNextNodeId(String value) {
            nextNodeId = value;
        }

        String currentNodeId() {
            return currentNodeId;
        }

        void setCurrentNodeId(String value) {
            currentNodeId = value;
        }

        public void reset() {
            currentNodeId = null;
            nextNodeId = null;
        }
    }
}
