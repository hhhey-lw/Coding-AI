package com.coding.core.manager;

import com.coding.core.manager.tool.ImageGenerateService;
import com.coding.core.manager.tool.MusicGenerateService;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.agent.flow.node.RoutingEdgeAction;
import com.coding.graph.core.agent.plan.PlanningTool;
import com.coding.graph.core.agent.plan.SupervisorAgent;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import com.coding.graph.core.state.strategy.ReplaceStrategy;
import com.coding.workflow.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncEdgeAction.edge_async;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;

/**
 * Agent 管理器
 */
@Slf4j
@Service
public class AgentManager {

    /**
     * 模型常量
     **/
    private static final String MODEL_QWEN_MAX = "qwen-max";
    private static final String MODEL_QWEN_PLUS = "qwen-plus";

    /**
     * 配置常量
     **/
    private static final int MAX_ITERATIONS = 10;
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_PLAN = "plan";
    private static final String KEY_STEP_PROMPT = "step_prompt";
    private static final String KEY_STEP_OUTPUT = "step_output";
    private static final String KEY_FINAL_OUTPUT = "final_output";
    private static final String KEY_CHOOSE = "choose";

    /**
     * Agent 名称常量
     **/
    private static final String AGENT_REACT = "react-agent";
    private static final String AGENT_PLANNING = "planning_agent";
    private static final String AGENT_SUPERVISOR = "supervisor_agent";
    private static final String AGENT_STEP_EXECUTING = "step_executing_agent";
    private static final String AGENT_QA = "qa_agent";

    /**
     * 工具名称常量
     **/
    private static final String TOOL_GENERATE_MUSIC = "generateMusic";
    private static final String TOOL_GENERATE_IMAGE = "generateImage";

    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final ToolCallbackResolver resolver;
    private final MusicGenerateService musicGenerateService;
    private final ImageGenerateService imageGenerateService;

    public AgentManager(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            ToolCallbackResolver resolver,
            MusicGenerateService musicGenerateService,
            ImageGenerateService imageGenerateService) {
        this.chatModel = OpenAiChatModel.builder().openAiApi(buildOpenAiApi(baseUrl, apiKey)).build();
        this.resolver = resolver;
        this.musicGenerateService = musicGenerateService;
        this.imageGenerateService = imageGenerateService;
        this.chatClient = createChatClient(null, MODEL_QWEN_MAX);
    }

    // =========================> 构建 React Agent <=========================

    /**
     * 构建 React Agent
     */
    public ReactAgent buildReactAgent() throws GraphStateException {
        List<ToolCallback> toolCallbacks = createToolCallbacks();

        return ReactAgent.builder()
                .name(AGENT_REACT)
                .modelName(MODEL_QWEN_MAX)
                .instruction(REACT_AGENT_SYSTEM_PROMPT)
                .inputKey(KEY_MESSAGES)
                .chatClient(chatClient)
                .tools(toolCallbacks)
                .resolver(resolver)
                .maxIterations(MAX_ITERATIONS)
                .stream(Boolean.TRUE)
                .build();
    }

    // =========================> 构建 Plan-Execute Agent <=========================

    /**
     * 构建 Plan-Execute Agent
     */
    public CompiledGraph buildPlanExecuteAgent() throws GraphStateException {
        ChatClient planningClient = createChatClient(PLANNING_SYSTEM_PROMPT, MODEL_QWEN_MAX);
        ChatClient executingClient = createChatClient(EXECUTING_SYSTEM_PROMPT, MODEL_QWEN_MAX);

        KeyStrategyFactory stateFactory = createStateFactory();
        SupervisorAgent supervisorAgent = new SupervisorAgent(PlanningTool.INSTANCE);

        ReactAgent planningAgent = createPlanningAgent(planningClient, Boolean.TRUE);
        ReactAgent stepAgent = createStepExecutingAgent(executingClient, Boolean.TRUE);
        ReactAgent chatAgent = createChatAgent(Boolean.TRUE);

        return buildPlanExecuteGraph(planningAgent, stepAgent, chatAgent, supervisorAgent, stateFactory);
    }

    /**
     * 创建规划 Agent
     */
    private ReactAgent createPlanningAgent(ChatClient planningClient, boolean isStream) throws GraphStateException {
        return ReactAgent.builder()
                .name(AGENT_PLANNING)
                .modelName(MODEL_QWEN_MAX)
                .description("负责根据用户的需求，制定详细的执行计划，每个计划包含多个有序的步骤，有序执行，能够调用外部工具。仅允许处理复杂任务")
                .instruction(PLANNING_SYSTEM_PROMPT)
                .inputKey(KEY_MESSAGES)
                .chatClient(planningClient)
                .resolver(resolver)
                .tools(List.of(PlanningTool.getFunctionToolCallback()))
                .stream(isStream)
                .build();
    }

    /**
     * 创建步骤执行 Agent
     */
    private ReactAgent createStepExecutingAgent(ChatClient executingClient, boolean isStream) throws GraphStateException {
        return ReactAgent.builder()
                .name(AGENT_STEP_EXECUTING)
                .modelName(MODEL_QWEN_MAX)
                .description("负责根据执行计划中的每个步骤，逐步完成任务。")
                .instruction(EXECUTING_SYSTEM_PROMPT)
                .inputKey(KEY_MESSAGES)
                .chatClient(executingClient)
                .resolver(resolver)
                .tools(createToolCallbacks())
                .stream(isStream)
                .build();
    }

    /**
     * 创建简单问答 Agent
     */
    private ReactAgent createChatAgent(boolean isStream) throws GraphStateException {
        return ReactAgent.builder()
                .name(AGENT_QA)
                .modelName(MODEL_QWEN_PLUS)
                .description("一个乐于助人的AI助手，擅长回答和用户进行日常沟通，无法调用外部工具。")
                .instruction("你是一个乐于助人的AI助手，擅长回答和用户进行日常沟通。请根据用户的提问进行回答。处理简单的问答任务。")
                .chatModel(chatModel)
                .stream(isStream)
                .inputKey(KEY_MESSAGES)
                .outputKey(KEY_CHOOSE)
                .build();
    }

    /**
     * 构建计划执行图
     */
    private CompiledGraph buildPlanExecuteGraph(ReactAgent planningAgent,
                                                ReactAgent stepAgent,
                                                ReactAgent chatAgent,
                                                SupervisorAgent supervisorAgent,
                                                KeyStrategyFactory stateFactory) throws GraphStateException {
        StateGraph graph = new StateGraph("plan_execute_graph", stateFactory)
                .addNode(AGENT_QA, chatAgent.asAsyncNodeAction(KEY_MESSAGES, KEY_MESSAGES))
                .addNode(AGENT_PLANNING, planningAgent.asAsyncNodeAction(KEY_MESSAGES, KEY_PLAN))
                .addNode(AGENT_SUPERVISOR, node_async(supervisorAgent))
                .addNode(AGENT_STEP_EXECUTING, stepAgent.asAsyncNodeAction(KEY_STEP_PROMPT, KEY_STEP_OUTPUT))

                .addConditionalEdges(START,
                        new RoutingEdgeAction(chatModel,
                                "你是一个意图分析大师，能够根据用户的提问结合各个Agent的特点，选择最合适的Agent，帮助用户完成任务",
                                KEY_MESSAGES,
                                List.of(chatAgent, planningAgent)),
                        Map.of(AGENT_QA, AGENT_QA,
                                AGENT_PLANNING, AGENT_PLANNING))
                .addEdge(AGENT_QA, END)
                .addEdge(AGENT_PLANNING, AGENT_SUPERVISOR)
                .addConditionalEdges(AGENT_SUPERVISOR, edge_async(supervisorAgent::think),
                        Map.of("continue", AGENT_STEP_EXECUTING, "end", END))
                .addEdge(AGENT_STEP_EXECUTING, AGENT_SUPERVISOR);

        return graph.compile();
    }

    // ==================== System Prompts ====================

    private static final String REACT_AGENT_SYSTEM_PROMPT = """
            ### 角色
            你是一个乐于助人的AI小助手，能够帮助用户完成任务，你能够调用工具来完成用户的制定任务。
                        
            ### 输出要求：
            1. <思考过程，你的推理过程，说明你为什么这么做，是否要调用工具，调用哪个工具等>。
            2. <你的下一步行动>。
            3. <总结工具调用的结果，可选，仅使用了工具才需要回复该部分>。
                        
            行动可以是：
            1. 调用一个工具，格式为：我需要使用xxx工具来完成这个任务。
            2. 直接回答用户，格式为：<你的回答内容>
                        
            ### 例子(<>表示解释)：
            用户：请你生成一张海滩落日照片。
            助手：
            	用户想要一张海滩落日的照片，但没有提供参考图。因为generateImage函数需要一个参考图片的URL地址作为参数，我将采取默认的处理方式，即不依赖于特定的参考图来生成新的图片。
            	接下来，首先，由于用户提供的提示词过于简单，我需要先丰富和优化用户提示词。然后，我会使用generateImage工具并仅提供描述性的提示词来生成图片。
            	<如果需要使用工具，你必须明确给出工具调用的消息请求，注意：这个要求你必须严格遵守，否则不会使用工具，对话则将停止在这里>
            工具响应：图片URL。
            助手：
            	我已经成功得到了工具生成的海滩落日照片，图片URL如下：http://www.test.com/example.png。本次任务完成。
            ### 注意
            - 如果你需要使用工具完成对应的任务，你可以调用工具来完成任务，注意，调用工具时你也需要告知用户你的推理过程。
            - 你必须遵守输出格式，即思考和行动是一对，只有使用了工具时你才需要简要总结工具回复。
            - 避免重复回复结果，避免冗长的解释和前端重复渲染。
            - 如果调用了工具，得到了工具的结果，你需要补充总结工具结果，然后继续思考和行动。
            - 你需要严格按照上述格式进行回复。
            """;


    private static final String PLANNING_SYSTEM_PROMPT = """
             # 任务规划助手
             ## 角色定位
             你是一个任务规划专家，**只负责制定计划，不负责执行**。你的任务是将用户的复杂需求拆解为简洁、可执行的步骤序列。
                        
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
            - **调用工具后**，不要继续执行或演示
            - 步骤描述要简洁有力，直指目标
            - `planId` 不需要在工具参数中提供，会自动生成
            """;

    private static final String EXECUTING_SYSTEM_PROMPT = """
                # 任务执行助手
                        
                ## 角色定位
                你是一名任务执行专家，负责**逐步执行**计划中的每个步骤。你每次只执行**一个步骤**，完成后立即停止，等待下一次调用。
                        
                ## 核心原则
                        
                ### ⚠️ 最重要的规则
                **你每次只能执行当前被分配的那一个步骤，绝对不能超前执行后续步骤！**
                        
                - ✅ 正确：只完成当前步骤，然后停止
                - ❌ 错误：完成当前步骤后，继续执行下一个、下下个步骤
                - ❌ 错误：在一次调用中完成多个步骤的任务
                        
                ### 执行方法
                1. **理解当前步骤**：仔细阅读系统分配给你的当前步骤描述
                2. **专注执行**：使用工具或生成内容来完成这一个步骤
                3. **立即停止**：完成后不要继续，不要预判下一步，不要提及后续步骤
                        
                ### 工具使用
                - 如果步骤需要调用工具（如生成图像、音乐），调用相应的工具
                - 如果步骤需要生成内容（如文本、代码），直接生成内容
                - 工具调用后等待结果，然后输出最终结果
                        
                ### 输出要求
                - 简洁明了地输出当前步骤的执行结果
                - **不要提及"接下来"、"下一步"、"然后"等字眼**
                - **不要输出计划中的其他步骤内容**
                - **不要重复之前步骤的结果**
                        
                ## 示例
                        
                ###逐步执行
                -我将按照给定的顺序**完成所提供计划中的每一步。
                -对于每一步，我将确定需要做什么，是否涉及工具使用，内容生成，数据处理或逻辑推理。
                -我会仔细执行该步骤，并记录输出或结果。
                        
                **❌ 错误的执行：**
                ```
                [调用 generateImage 工具]
                我已经生成了落日沙滩的图片：[图片URL]
                接下来，我将创作歌词：[开始写歌词...]  ← 错误！不要执行下一步！
                然后生成音乐：[调用音乐工具...]  ← 错误！不要超前执行！
                ```
                        
                进度监控和自检
                -在执行每个步骤后，我将确保结果是有效的，完整的，并与步骤的意图一致。
                -如果发生意外情况（如信息缺失，工具错误），我会适应或暂停重新评估，始终保持在整体计划的范围内。
                -除非明确指示或阻止，否则我不会偏离步骤顺序。
                        
                结果编译
                -所有步骤完成后，我将把输出整合成一个符合原始用户要求的连贯的最终结果。
                -我将确保最终交付的内容经过润色，一致，并准备好交付或演示。
                        
                ## 重要提醒
                - 你会收到一个明确的步骤描述和上下文
                - 你**只需要完成这一个步骤**
                - 完成后**立即结束**，不要继续
                - 系统会自动调度下一步骤，你不需要担心
                - **专注当前，不要超前！**
            """;

    // =========================> 模型调用准备函数 <=========================

    /**
     * 构建 OpenAiApi，添加响应过滤器处理 SSE 流(将思考内容使用<think></think>标签对包裹放入回复内容中)
     */
    private OpenAiApi buildOpenAiApi(String baseUrl, String apiKey) {
        ExchangeFilterFunction responseFilter = ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            // 处理SSE事件流，将reasoning_content提取并放入metadata中
            Flux<DataBuffer> modifiedBody = clientResponse.bodyToFlux(DataBuffer.class)
                    .map(buf -> {
                        byte[] bytes = new byte[buf.readableByteCount()];
                        buf.read(bytes);
                        DataBufferUtils.release(buf);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .flatMap(eventString -> {
                        String[] list = eventString.split("\\n", -1);
                        List<String> lines = new ArrayList<>();
                        for (String line : list) {
                            if (line.startsWith("data: ")) {
                                String jsonPart = line.substring(6).trim();
                                if (JSONUtil.isTypeJSON(jsonPart) && !"data: [DONE]".equals(line)) {
                                    JSONObject retJson;
                                    try {
                                        retJson = JSONUtil.parseObj(jsonPart);
                                    } catch (Exception e) {
                                        lines.add(line);
                                        break;
                                    }
                                    // 提取思考内容并放置到回复内容中，使用<think></think>包装
                                    JSONArray choices = retJson.getJSONArray("choices");
                                    for (int i = 0; i < choices.size(); i++) {
                                        JSONObject choice = choices.getJSONObject(i);
                                        if (choice == null) {
                                            break;
                                        }
                                        JSONObject delta = choice.getJSONObject("delta");
                                        if (delta == null) {
                                            break;
                                        }
                                        String reasoningContent = delta.getStr("reasoning_content");
                                        if (StringUtils.isNotBlank(reasoningContent)) {
                                            // 思考内容和正文内容是互斥的，将思考内容设置到delta的content字段
                                            delta.set("content", "<think>" + reasoningContent + "</think>");
                                            // 移除reasoning_content字段，避免重复
                                            delta.remove("reasoning_content");
                                        }
                                    }
                                    String modifiedJson = retJson.toString();
                                    lines.add("data: " + modifiedJson);
                                } else {
                                    lines.add(line);
                                }
                            } else {
                                lines.add(line);
                            }
                        }

                        String finalLine = StringUtils.join(lines, "\n");
                        return Mono.just(finalLine);
                    })
                    .map(str -> {
                        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                        return new DefaultDataBufferFactory().wrap(bytes);
                    });

            // 创建新的ClientResponse，移除CONTENT_LENGTH头
            ClientResponse modifiedResponse = ClientResponse.from(clientResponse)
                    .headers(headers -> headers.remove(HttpHeaders.CONTENT_LENGTH))
                    .body(modifiedBody)
                    .build();

            return Mono.just(modifiedResponse);
        });

        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .headers(ApiUtils.getBaseHeaders())
                .webClientBuilder(WebClient.builder().filter(responseFilter))
                .build();
    }

    /**
     * 创建 ChatClient
     */
    private ChatClient createChatClient(String systemPrompt, String modelName) {
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(modelName)
                                .internalToolExecutionEnabled(Boolean.FALSE)
                                .parallelToolCalls(Boolean.FALSE)
                                .build()
                );

        if (systemPrompt != null) {
            builder.defaultSystem(systemPrompt);
        }

        return builder.build();
    }

    /**
     * 创建工具回调列表
     */
    private List<ToolCallback> createToolCallbacks() {
        return List.of(
                FunctionToolCallback.builder(TOOL_GENERATE_MUSIC, musicGenerateService)
                        .description("根据风格提示词和歌词内容，生成一段音乐，并返回音乐的URL地址")
                        .inputType(MusicGenerateService.Request.class)
                        .build(),
                FunctionToolCallback.builder(TOOL_GENERATE_IMAGE, imageGenerateService)
                        .description("根据图片提示词和参考图生成对应的图片，并返回图片的URL地址")
                        .inputType(ImageGenerateService.Request.class)
                        .build()
        );
    }

    /**
     * 创建状态工厂
     */
    private KeyStrategyFactory createStateFactory() {
        return () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put(KEY_PLAN, new ReplaceStrategy());
            keyStrategyHashMap.put(KEY_STEP_PROMPT, new ReplaceStrategy());
            keyStrategyHashMap.put(KEY_STEP_OUTPUT, new ReplaceStrategy());
            keyStrategyHashMap.put(KEY_FINAL_OUTPUT, new ReplaceStrategy());
            keyStrategyHashMap.put(KEY_MESSAGES, new ReplaceStrategy());
            return keyStrategyHashMap;
        };
    }

}
