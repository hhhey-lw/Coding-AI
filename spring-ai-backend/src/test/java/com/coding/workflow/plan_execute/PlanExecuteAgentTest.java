package com.coding.workflow.plan_execute;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import com.coding.graph.core.state.strategy.ReplaceStrategy;
import com.coding.workflow.agent.ImageGenerateService;
import com.coding.workflow.agent.MusicGenerateService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncEdgeAction.edge_async;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;

@SpringBootTest
public class PlanExecuteAgentTest {
    @Resource
    private ChatModel chatModel;
    private ChatClient planningClient;
    private ChatClient executingClient;
    private CompiledGraph compiledGraph;
    @Resource
    private ToolCallbackResolver resolver;

    public void init() throws GraphStateException, InterruptedException {
        this.planningClient = ChatClient.builder(chatModel)
            .defaultSystem(PLANNING_SYSTEM_PROMPT)
            .defaultAdvisors(new SimpleLoggerAdvisor())
//            .defaultToolCallbacks(List.of(PlanningTool.getFunctionToolCallback()))
            .defaultOptions(OpenAiChatOptions.builder()
                    .model("qwen-max")
                    .internalToolExecutionEnabled(false)
                    .build())
            .build();

        List<ToolCallback> toolCallbacks = List.of(FunctionToolCallback.builder("generateMusic", new MusicGenerateService())
                .description("根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
                .inputType(MusicGenerateService.Request.class)
                .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                .description("根据提示词和参考图生成图片，并返回图片的URL地址")
                .inputType(ImageGenerateService.Request.class)
                .build());

        this.executingClient = ChatClient.builder(chatModel)
            .defaultSystem(EXECUTING_SYSTEM_PROMPT)
            .defaultAdvisors(new SimpleLoggerAdvisor())
//            .defaultToolCallbacks(toolCallbacks)
            .defaultOptions(OpenAiChatOptions.builder()
                    .model("qwen-max")
                    .internalToolExecutionEnabled(false)
                    .build())
            .build();

        initGraph();

    }

    public void initGraph() throws GraphStateException, InterruptedException {

        KeyStrategyFactory stateFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("plan", new ReplaceStrategy());
            keyStrategyHashMap.put("step_prompt", new ReplaceStrategy());
            keyStrategyHashMap.put("step_output", new ReplaceStrategy());
            keyStrategyHashMap.put("final_output", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        SupervisorAgent supervisorAgent = new SupervisorAgent(PlanningTool.INSTANCE);
        boolean isStream = true;

        ReactAgent planningAgent = ReactAgent.build(planningClient, ReactAgent.builder()
                    .name("planning_agent")
                    .modelName("qwen-max")
                    .description("负责根据用户的需求，制定详细的执行计划，每个计划包含多个有序的步骤。")
                    .instruction(PLANNING_SYSTEM_PROMPT)
                    .inputKey("messages")
                    .resolver(resolver)
                    .tools(List.of(PlanningTool.getFunctionToolCallback()))
                    .stream(isStream)
                .build());
        planningAgent.getAndCompileGraph();

        List<ToolCallback> toolCallbacks = List.of(FunctionToolCallback.builder("generateMusic", new MusicGenerateService())
                .description("根据提示词和歌词，生成一段音乐，并返回音乐的URL地址")
                .inputType(MusicGenerateService.Request.class)
                .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                .description("根据提示词和参考图生成图片，并返回图片的URL地址")
                .inputType(ImageGenerateService.Request.class)
                .build());

        ReactAgent stepAgent = ReactAgent.build(executingClient, ReactAgent.builder()
                    .name("step_executing_agent")
                    .modelName("qwen-max")
                    .description("负责根据执行计划中的每个步骤，逐步完成任务。")
                    .instruction(EXECUTING_SYSTEM_PROMPT)
                    .inputKey("messages")
                    .resolver(resolver)
                    .tools(toolCallbacks)
                    .stream(isStream)
                .build());
        stepAgent.getAndCompileGraph();

        StateGraph graph = new StateGraph("plan_execute_graph", stateFactory)
                .addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
                .addNode("supervisor_agent", node_async(supervisorAgent))
                .addNode("step_executing_agent", stepAgent.asAsyncNodeAction("step_prompt", "step_output"))

                .addEdge(START, "planning_agent")
                .addEdge("planning_agent", "supervisor_agent")
                .addConditionalEdges("supervisor_agent", edge_async(supervisorAgent::think),
                        Map.of("continue", "step_executing_agent", "end", END))
                .addEdge("step_executing_agent", "supervisor_agent");

        this.compiledGraph = graph.compile();
    }

    @Test
    public void testPlanExecute() throws GraphStateException, InterruptedException, GraphRunnerException, ExecutionException {
        init();

        AsyncGenerator<NodeOutput> output = this.compiledGraph.stream(Map.of("input", "帮我创作一段轻快的电子音乐，歌词是关于夏日海滩的快乐时光。并创作一幅对于音乐的海报"));
        processStream(output, Sinks.many().unicast().onBackpressureBuffer()).get();
//        Optional<OverAllState> output = this.compiledGraph.invoke(Map.of("input", "帮我创作一段轻快的电子音乐，歌词是关于夏日海滩的快乐时光。并创作一幅对于音乐的海报"));
//        System.out.println(output.get());
        Thread.sleep(10000L);
    }

    CompletableFuture<Void> processStream(AsyncGenerator<NodeOutput> generator,
                                          Sinks.Many<ServerSentEvent<String>> sink) {
        return generator.forEachAsync(output -> {
            try {
                String nodeName = output.getNode();
                String content;
                if (output instanceof StreamingOutput streamingOutput) {
                    content = JSONUtil.toJsonStr(Map.of(nodeName, streamingOutput.getChatResponse().getResult().getOutput().getText()));
                }
                else {
                    JSONObject nodeOutput = new JSONObject();
                    nodeOutput.put("data", output.getState().data());
                    nodeOutput.put("node", nodeName);
                    content = JSONUtil.toJsonStr(nodeOutput);
                }
                System.out.println("Received output: " + content);
                sink.tryEmitNext(ServerSentEvent.builder(content).build());
            }
            catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAccept(v -> {
            // 正常完成
            sink.tryEmitComplete();
        }).exceptionally(e -> {
            sink.tryEmitError(e);
            return null;
        });
    }

    //


    private static final String PLANNING_SYSTEM_PROMPT = """
            # Manus AI Assistant Capabilities

			## Overview
			I am an AI assistant designed to help users with a wide range of tasks, for every task given by the user, I should make detailed plans on how to complete the task step by step. That means the output should be structured steps in sequential.

			## Task Approach Methodology

			### Understanding Requirements
			- Analyzing user requests to identify core needs
			- Asking clarifying questions when requirements are ambiguous
			- Breaking down complex requests into manageable components
			- Identifying potential challenges before beginning work

			### Planning and Execution
			- Creating structured plans for task completion
			- Selecting appropriate tools and approaches for each step
			- Executing steps methodically while monitoring progress
			- Adapting plans when encountering unexpected challenges
			- Providing regular updates on task status
			- 计划应该简练明了，不要包含多余的信息

			### Quality Assurance
			- Verifying results against original requirements
			- Testing code and solutions before delivery
			- Documenting processes and solutions for future reference
			- Seeking feedback to improve outcomes

			### Tool usage
			You are given access to a planning tool, which can be used to generate a plan for the task given by the user. The tool will return a structured plan with steps in sequential.

			## Example output
			Task given by the user: 帮我写一封中文商务邮件，主题是「关于下周项目评审会议安排」，收件人是部门全体同事，需要包含会议时间、地点、议程和准备事项。
            
            ### 输出格式
            输出内容严格要求如下面的示例，JSON对象，包括工具返回的planId和你定制的计划steps。
            
			Output example:
			```json
			{
				"planId": "1",
				"steps": [
				"1. 确认邮件目标：写一封中文商务邮件，收件人为部门全体同事",
                "2. 明确邮件主题：关于下周项目评审会议安排",
                "3. 确定会议基本信息：包括会议时间（如：下周三上午10点）、会议地点（如：线上腾讯会议 / 线下会议室301）、议程（如：项目A进展汇报、项目B风险讨论、下一步计划）",
                "4. 列出参会人员需要提前准备的事项（如：各自项目的进度文档、风险点清单、需要协调的资源）",
                "5. 组织语言，按照商务邮件标准格式撰写邮件，包括：称呼、正文（会议信息 + 议程 + 准备事项）、结尾敬语与发件人信息",
                "6. 检查邮件内容是否清晰、礼貌、专业，确保无遗漏关键信息",
                "7. 输出最终完整邮件内容，准备发送"
				]
			}
			```
            """;

    private static final String EXECUTING_SYSTEM_PROMPT = """
        # Manus AI Assistant - Execution Phase Capabilities

        ## Overview
        I am an AI assistant currently in the **execution phase** of a task. My goal is to carry out the task step by step, following a pre-defined, structured plan that contains clear and sequential steps.
        
        I will focus exclusively on **executing each step in order**, using appropriate methods, tools, or reasoning as needed. My execution must be methodical, traceable, and aligned with the original plan and user's intent.
        
        ## Execution Approach Methodology
        
        ### Step-by-Step Execution
        - I will go through each step in the provided plan **in the given order**.
        - For each step, I will determine what needs to be done, whether it involves tool usage, content generation, data processing, or logical reasoning.
        - I will execute that step carefully and record the output or result.
        
        ### Tool Usage & Actions
        - If a step requires using a specific tool (e.g., generating an image, querying data, making a chart), I will invoke the correct tool with accurate input parameters.
        - If a step involves generating content (like text, summaries, code), I will produce it carefully and ensure it meets professional standards.
        - If the step is informational or logical (e.g., "verify data", "check completeness"), I will perform the check explicitly and note the outcome.
        
        ### Progress Monitoring & Self-Checking
        - After executing each step, I will ensure the result is valid, complete, and aligns with the step's intent.
        - If something unexpected happens (e.g., missing info, tool error), I will adapt or pause to reassess, always staying within the scope of the overall plan.
        - I will not deviate from the step sequence unless explicitly instructed or blocked.
        
        ### Result Compilation
        - After all steps are completed, I will integrate the outputs into a **coherent final result** that fulfills the original user request.
        - I will ensure the final deliverable is polished, consistent, and ready for delivery or presentation.
        
        ### Quality & Accountability
        - Each step output will be self-reviewed for accuracy, clarity, and relevance.
        - I will make sure nothing is skipped, and all steps are properly executed and accounted for.
    """;

}
