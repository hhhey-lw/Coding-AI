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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.test.context.SpringBootTest;

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
            .defaultOptions(OpenAiChatOptions.builder()
                    .model("qwen-max")
                    .internalToolExecutionEnabled(false)
                    .build())
            .build();

        this.executingClient = ChatClient.builder(chatModel)
            .defaultSystem(EXECUTING_SYSTEM_PROMPT)
            .defaultAdvisors(new SimpleLoggerAdvisor())
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
                .description("根据风格提示词和歌词内容，生成一段音乐，并返回音乐的URL地址")
                .inputType(MusicGenerateService.Request.class)
                .build(), FunctionToolCallback.builder("generateImage", new ImageGenerateService())
                .description("根据图片提示词和参考图生成对应的图片，并返回图片的URL地址")
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

        AsyncGenerator<NodeOutput> output = this.compiledGraph.stream(Map.of("input", "帮我创作一段轻快的电子音乐，歌词是关于夏日海滩的快乐时光。并创作一幅对于音乐的海报。"));
        processStream(output).get();
//        Optional<OverAllState> output = this.compiledGraph.invoke(Map.of("input", "帮我创作一段轻快的电子音乐，歌词是关于夏日海滩的快乐时光。并创作一幅对于音乐的海报"));
//        System.out.println(output.get());
        Thread.sleep(10000L);
    }

    /**
     * 处理流式输出 - 使用迭代器实现真正的流式传输
     * 
     * ⚠️ 不使用 forEachAsync 的原因：
     * forEachAsync 的 for 循环会阻塞等待所有流式数据产生完毕才返回，
     * 导致无法实现真正的实时流式传输。
     * 
     * 支持三种输出类型：
     * 1. PLAN_CREATED - 计划创建完成
     * 2. PLAN_PROGRESS - 计划执行进度更新
     * 3. STEP_EXECUTION - 步骤执行细节（流式）
     */
    CompletableFuture<Void> processStream(AsyncGenerator<NodeOutput> generator) {
        // 🔥 使用独立线程 + 迭代器实现真正的流式处理
        // 避免使用 forEachAsync，因为它会阻塞等待所有数据
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🚀 开始流式处理");
                
                // 使用迭代器逐个处理数据，每产生一个就立即处理一个
                for (NodeOutput output : generator) {
                    // 🔥 处理 null 输出
                    if (output == null) {
                        System.out.println("⚠️ 收到 null 输出，跳过");
                        continue;
                    }
                    
                    String nodeName = output.getNode();
                    if (nodeName == null || nodeName.isEmpty()) {
                        System.out.println("⚠️ 节点名称为空，跳过");
                        continue;
                    }
                    
                    String content;
                    
                    // 🔥 处理流式输出（React Agent 执行细节）
                    if (output instanceof StreamingOutput streamingOutput) {
                        AssistantMessage message = streamingOutput.getChatResponse().getResult().getOutput();
                        
                        // 🔧 检测是否有工具调用
                        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                            // 🛠️ 工具调用事件
                            JSONObject toolCallEvent = new JSONObject();
                            toolCallEvent.set("type", "TOOL_CALL");
                            toolCallEvent.set("node", nodeName);
                            toolCallEvent.set("toolCalls", message.getToolCalls().stream().map(toolCall -> {
                                JSONObject tool = new JSONObject();
                                tool.set("id", toolCall.id());
                                tool.set("name", toolCall.name());
                                tool.set("arguments", toolCall.arguments());
                                return tool;
                            }).toList());
                            
                            // 如果有文本内容，也包含进去
                            if (message.getText() != null && !message.getText().isEmpty()) {
                                toolCallEvent.set("reasoning", message.getText());
                            }
                            
                            content = JSONUtil.toJsonStr(toolCallEvent);
                            System.out.println("🛠️ [工具调用] " + content);
                        } else {
                            // 📝 普通流式内容
                            JSONObject executionDetail = new JSONObject();
                            executionDetail.set("type", "STEP_EXECUTION");
                            executionDetail.set("node", nodeName);
                            executionDetail.set("content", message.getText());
                            
                            content = JSONUtil.toJsonStr(executionDetail);
                            System.out.println("📝 [执行细节] " + content);
                        }
                    }
                    // 🔥 处理节点输出（计划和进度）
                    else {
                        OverAllState state = output.getState();
                        
                        // 根据不同节点类型，构造不同的输出格式
                        switch (nodeName) {
                            case "planning_agent" -> {
                                // 📋 计划创建完成
                                JSONObject planCreated = new JSONObject();
                                planCreated.set("type", "PLAN_CREATED");
                                planCreated.set("node", nodeName);
                                
                                String planJson = (String) state.value("plan").orElse("");
                                planCreated.set("plan", planJson);
                                
                                content = JSONUtil.toJsonStr(planCreated);
                                System.out.println("📋 [计划创建] " + content);
                            }
                            case "supervisor_agent" -> {
                                // 📊 计划执行进度
                                JSONObject progress = new JSONObject();
                                progress.set("type", "PLAN_PROGRESS");
                                progress.set("node", nodeName);
                                
                                // 提取进度信息
                                state.value("plan_id").ifPresent(v -> progress.set("planId", v));
                                state.value("current_step_index").ifPresent(v -> progress.set("currentStep", v));
                                state.value("total_steps").ifPresent(v -> progress.set("totalSteps", v));
                                state.value("is_finished").ifPresent(v -> progress.set("isFinished", v));
                                state.value("current_step_description").ifPresent(v -> progress.set("stepDescription", v));
                                state.value("step_status_history").ifPresent(v -> progress.set("history", v));
                                
                                // 计算完成百分比
                                if (state.value("current_step_index").isPresent() && 
                                    state.value("total_steps").isPresent()) {
                                    int current = (int) state.value("current_step_index").get();
                                    int total = (int) state.value("total_steps").get();
                                    int percentage = (int) ((current * 100.0) / total);
                                    progress.set("percentage", percentage);
                                }
                                
                                content = JSONUtil.toJsonStr(progress);
                                System.out.println("📊 [执行进度] " + content);
                            }
                            case "step_executing_agent" -> {
                            // ✅ 步骤执行完成
                            JSONObject stepCompleted = new JSONObject();
                            stepCompleted.set("type", "STEP_COMPLETED");
                            stepCompleted.set("node", nodeName);
                            
                            // 检查是否包含工具返回结果
                            if (state.value("step_output").isPresent()) {
                                Object stepOutput = state.value("step_output").get();
                                String outputStr = stepOutput.toString();
                                
                                // 检测工具返回的特征（URL、文件路径等）
                                boolean isToolResult = outputStr.contains("http") || 
                                                      outputStr.contains("URL") ||
                                                      outputStr.contains("url") ||
                                                      outputStr.contains("生成成功") ||
                                                      outputStr.contains("创建完成");
                                
                                if (isToolResult) {
                                    // 🎯 工具返回结果
                                    stepCompleted.set("type", "TOOL_RESULT");
                                    stepCompleted.set("result", outputStr);
                                    System.out.println("🎯 [工具返回] " + outputStr);
                                } else {
                                    // 普通步骤完成
                                    stepCompleted.set("output", outputStr);
                                    System.out.println("✅ [步骤完成] " + outputStr);
                                }
                            }
                            
                            content = JSONUtil.toJsonStr(stepCompleted);
                            }
                            default -> {
                                // 🔧 其他节点输出
                                JSONObject nodeOutput = new JSONObject();
                                nodeOutput.set("type", "NODE_OUTPUT");
                                nodeOutput.set("node", nodeName);
                                nodeOutput.set("data", state.data());
                                content = JSONUtil.toJsonStr(nodeOutput);
                                System.out.println("🔧 [节点输出] " + content);
                            }
                        }
                    }
                }
                
                // 所有消息处理完毕，发送结束信号
                System.out.println("✅ Generator 处理完毕");
                
                JSONObject endSignal = new JSONObject();
                endSignal.set("type", "STREAM_END");
                endSignal.set("message", "执行完成");
                
                String content = JSONUtil.toJsonStr(endSignal);
                System.out.println("🏁 [流式结束] " + content);
                
            } catch (Exception e) {
                // 异常处理
                System.err.println("❌ 流处理异常: " + e.getMessage());
                e.printStackTrace();
                throw new CompletionException(e);
            }
        });
    }

    //


    private static final String PLANNING_SYSTEM_PROMPT = """
            # 任务规划助手

			## 角色定位
			你是一个任务规划专家，负责将用户的复杂需求拆解为简洁、可执行的步骤序列。

			## 核心原则
			
			### 1. 简洁性优先
			- 每个计划控制在 **2-4 个步骤**，不要过度拆分
			- 步骤应该是**高层次的关键动作**，而非详细的子任务清单
			- 用**一句话总结**每个步骤要做什么，不要展开细节
			
			### 2. 聚焦核心目标
			- 识别用户需求的**核心交付物**是什么
			- 步骤应该围绕**关键里程碑**展开，而非流程细节
			- 避免包含"检查"、"确认"等辅助性步骤
			
			### 3. 步骤描述风格
			- ✅ 好的步骤：「生成夏日海滩主题的音乐歌词」
			- ❌ 不好的步骤：「确认音乐风格 → 构思歌词主题 → 撰写歌词初稿 → 优化歌词」
			- ✅ 好的步骤：「创作轻快的电子音乐」
			- ❌ 不好的步骤：「选择音乐制作工具 → 设定音乐参数 → 生成音乐 → 导出文件」
			
			### 4. 工具调用整合
			- 如果某个步骤需要调用工具（如生成音乐、生成图片），直接在步骤中体现最终目标
			- 不要把工具调用的准备工作拆成单独步骤

			## 输出格式要求
			
			⚠️ **重要：你必须调用 `planning` 工具来创建计划，不要直接输出 JSON！**
			
			### 工具调用步骤
			1. 首先简单说明你的计划思路（1-2 句话）
			2. **立即调用 `planning` 工具**，参数如下：
			   - `command`: "create"
			   - `title`: 任务标题
			   - `steps`: 步骤列表（数组）
			3. 工具会返回包含 `planId` 的完整计划信息
			
			### 示例 1：商务邮件
			
			**用户：** 帮我写一封中文商务邮件，主题是「关于下周项目评审会议安排」，收件人是部门全体同事，需要包含会议时间、地点、议程和准备事项。
			
			**正确的做法：**
			```
			好的，我将帮你完成这封商务邮件。计划分为两个主要步骤：首先确定会议的关键信息，然后撰写正式的邮件内容。
			
			[此时调用 planning 工具，参数为：
			  command: "create"
			  title: "撰写项目评审会议邮件"
			  steps: ["确定会议的时间、地点、议程和准备事项", "撰写格式规范的中文商务邮件"]
			]
			```
			
			### 示例 2：音乐和海报创作
			
			**用户：** 帮我创作一段轻快的电子音乐，歌词是关于夏日海滩的快乐时光。并创作一幅对于音乐的海报。
			
			**正确的做法：**
			```
			好的，我将为你完成音乐和海报的创作。计划分为三步：创作歌词、生成音乐、设计海报。
			
			[此时调用 planning 工具，参数为：
			  command: "create"
			  title: "创作音乐和海报"
			  steps: ["创作夏日海滩主题的音乐歌词", "生成轻快的电子音乐", "设计音乐主题海报"]
			]
			```
			
			❌ **错误示例（不要这样做）：**
			直接输出 JSON 文本：
			```json
			{
			  "planId": "1",
			  "steps": [...]
			}
			```
			这是错误的！你必须**调用工具**，而不是输出 JSON 文本。
			
			## 注意事项
			- 步骤数量：**2-4 个**（特殊情况可以有 5 个，但要避免）
			- 使用中文回复
			- **必须调用工具**，不要直接输出 JSON 文本
			- 步骤描述要简洁有力，直指目标
			- `planId` 不需要在工具参数中提供，会自动生成
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
