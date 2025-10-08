package com.coding.graph;

import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.GraphLifecycleListener;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.command.Command;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactoryBuilder;
import com.coding.graph.core.utils.EdgeMappings;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;

@Slf4j
public class GraphFuncTest {
    public static void main(String[] args) throws GraphStateException {
        // 基本、条件、并行工作流测试
//        testBaseWorkflow();
//        testConditionalWorkflow();
//        testParallelWorkflow();
//        testSubGraphWorkflow();

        // 带节点配置、上下文传参的工作流测试
        testNodeParamWorkflow();

        // TODO 测试画布节点转工作流
//        testCanvasWorkflow();
    }

    private static void testSubGraphWorkflow() throws GraphStateException {
        // TODO 测试子图工作流
        AtomicInteger i = new AtomicInteger(1);

        StateGraph subGraph = new StateGraph("子工作流", KeyStrategyFactoryBuilder.builder().build());
        subGraph.addNode("sub_agent", node_async((state -> {
            System.out.println("sub_agent do execute");
            return Map.of("sub_agent", "sub_agent");
        })));
        subGraph.addNode("sub_choose", node_async((state -> {
            System.out.println("sub_choose do execute");
            return Map.of("sub_choose", "sub_choose");
        })));
        subGraph.addEdge(START, "sub_agent");
        subGraph.addEdge("sub_agent", "sub_choose");
        subGraph.addConditionalEdges("sub_choose", ((overAllState, runnableConfig) ->  {
                    System.out.println("sub 条件边：判断 i=" + i.getAndIncrement());
                    if (i.get() == 3) {
                        System.out.println("sub 条件边：结束子图");
                        return CompletableFuture.completedFuture(new Command("end"));
                    }
                    return CompletableFuture.completedFuture(new Command("continue"));
                }),
                EdgeMappings.builder()
                        .to("sub_agent", "continue")
                        .to(END, "end")
                .build());

        StateGraph workflow = new StateGraph("主工作流", KeyStrategyFactoryBuilder.builder().build());
        workflow.addNode("main_agent", node_async((state -> {
            System.out.println("main_agent do execute");
            return Map.of("main_agent", "main_agent");
        })));

        workflow.addNode("sub_graph", subGraph);
        workflow.addNode("aggregate", node_async((state ->  {
            System.out.println("aggregate do execute");
            return Map.of("aggregate", "aggregate");
        })));

        workflow.addEdge(START, "main_agent");
        workflow.addEdge("main_agent", "sub_graph");
        workflow.addEdge("sub_graph", "aggregate");
        workflow.addEdge("aggregate", END);

        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder()
                .withLifecycleListener(new GraphLifecycleListener() {
                    @Override
                    public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
                        System.out.println("图: " + nodeId + " 开始执行");
                    }

                    @Override
                    public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
                        System.out.println("图: " + nodeId + " 执行完成");
                    }

                    @Override
                    public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
                        System.out.println("节点: " + nodeId + ", state: " + state);
                    }
                })
                .build());
        compiledGraph.invoke(Map.of("input", "input")).ifPresent(state -> {
            System.out.println(state.data());
        });
    }

    private static StateGraph testNodeParamWorkflow() throws GraphStateException {
        // TODO 测试带节点配置、上下文传参的工作流
        StateGraph workflow = new StateGraph("测试工作流", KeyStrategyFactoryBuilder.builder().build());

        workflow.addNode("agent1", AsyncNodeActionWithConfig.node_async(((state, config) -> {
            System.out.println("agent1 do execute");
            config.metadata("agent1").ifPresent(System.out::println);
            return Map.of("agent1", "agent1");
        })));

        workflow.addNode("agent2", AsyncNodeActionWithConfig.node_async(((state, config) -> {
            System.out.println("agent2 do execute");
            config.metadata("agent2").ifPresent(System.out::println);
            System.out.println("Context from state: " + state.value("agent1", String.class).orElse("no context"));
            return Map.of("agent2", "agent2");
        })));

        workflow.addEdge(START, "agent1");
        workflow.addEdge("agent1", "agent2");
        workflow.addEdge("agent2", END);

        CompiledGraph compiledGraph = workflow.compile();

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .metadata(Map.of("agent1", "agent1Param", "agent2", "agent2Param"))
                .build();

        Optional<OverAllState> invoke = compiledGraph.invoke(Map.of("input", "input"), runnableConfig);
        invoke.ifPresent(System.out::println);

        return workflow;
    }

    private static void testParallelWorkflow() throws GraphStateException {
        // TODO 测试并行分支工作流
        StateGraph workflow = new StateGraph("测试工作流", KeyStrategyFactoryBuilder.builder().build());

        workflow.addNode("parallel", node_async((state ->  {
            System.out.println("parallel do execute");
            return Map.of("parallel", "parallel");
        })));

        workflow.addNode("agent_1", node_async((state ->  {
            System.out.println("agent_1 do execute");
            return Map.of("agent_1", "agent_1");
        })));
        workflow.addNode("agent_2", node_async((state ->  {
            System.out.println("agent_2 do execute");
            return Map.of("agent_2", "agent_2");
        })));
        workflow.addNode("agent_3", node_async((state ->  {
            System.out.println("agent_3 do execute");
            return Map.of("agent_3", "agent_3");
        })));

        workflow.addNode("aggregate", node_async((state ->  {
            System.out.println("aggregate do execute");
            return Map.of("aggregate", "aggregate");
        })));

        workflow.addEdge(START, "parallel");
        workflow.addEdge("parallel", "agent_1");
        workflow.addEdge("parallel", "agent_2");
        workflow.addEdge("parallel", "agent_3");
        workflow.addEdge("agent_1", "aggregate");
        workflow.addEdge("agent_2", "aggregate");
        workflow.addEdge("agent_3", "aggregate");
        workflow.addEdge("aggregate", END);

        CompiledGraph compiledGraph = workflow.compile();
        Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", "input"));

        result.ifPresent(state -> {
            System.out.println(state.data());
        });
    }

    private static void testConditionalWorkflow() throws GraphStateException {
        // TODO 测试条件分支工作流
        AtomicInteger i = new AtomicInteger();

        StateGraph workflow = new StateGraph("测试工作流", KeyStrategyFactoryBuilder.builder().build());

        workflow.addNode("agent", node_async((state) -> {
            System.out.println("agent: " + state.data());
            return Map.of("agent", "agent");
        }));
        workflow.addNode("action", node_async((state) -> {
            System.out.println("action: " + i);
            i.addAndGet(1);
            return Map.of("action", i);
        }));

        workflow.addEdge(START, "agent");
        workflow.addConditionalEdges("agent", ((overAllState, runnableConfig) ->  {
                    System.out.println("条件判断: \n" + overAllState.data());
                    if (i.get() == 3) {
                        return CompletableFuture.completedFuture(new Command("end"));
                    }
                    return CompletableFuture.completedFuture(new Command("continue"));
                }),EdgeMappings.builder()
                        .to("action", "continue")
                        .to(END, "end").build()
                );

        workflow.addEdge("action", "agent");

        CompiledGraph compiledGraph = workflow.compile();
        Optional<OverAllState> result = compiledGraph.invoke(Map.of("START", "start"));
        System.out.println(result.get());
    }


    private static void testBaseWorkflow() throws GraphStateException {
        StateGraph workflow = new StateGraph("测试工作流", KeyStrategyFactoryBuilder.builder()
                .addStrategy("prop1", KeyStrategy.APPEND)
                .build());

        workflow.addEdge(START, "agent_1");

        workflow.addNode("agent_1", node_async((state) -> {
            System.out.println("agent_1: \n" + state);
            return Map.of("prop1", List.of("test"));
        }));


        workflow.addEdge("agent_1", "agent_2");

        workflow.addNode("agent_2", node_async((state) -> {
            System.out.println("agent_2: \n" + state);
            List msg = Optional.ofNullable(state.value("prop1").get())
                    .filter(List.class::isInstance)
                    .map(List.class::cast)
                    .orElse(List.of("not found"));
            System.out.println("msg = " + msg);
            return Map.of("prop2", "test",
            "prop1", List.of("append"));
        }));

        workflow.addEdge("agent_2", END);

        CompiledGraph app = workflow.compile();
        Optional<OverAllState> result = app.invoke(Map.of());
        System.out.println("result = " + result);
    }
}
