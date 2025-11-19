package com.coding.graph.core.agent;

import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.action.AsyncNodeAction;
import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.node.impl.LlmNode;
import com.coding.graph.core.node.impl.ToolNode;
import com.coding.graph.core.state.OverAllState;
import com.coding.graph.core.state.strategy.KeyStrategy;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import com.coding.graph.core.state.strategy.KeyStrategyFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.*;
import java.util.function.Function;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncEdgeAction.edge_async;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;

/**
 * ReAct（推理-行动）智能体实现。
 * 该智能体通过迭代推理和工具调用来解决复杂任务。
 */
@Getter
@Setter
public class ReactAgent extends BaseAgent {

	/** LLM 节点，负责与大语言模型交互 */
	private final LlmNode llmNode;

	/** 工具节点，负责执行工具调用 */
	private final ToolNode toolNode;

	/** 状态图，定义智能体的执行流程 */
	private final StateGraph graph;

	/** 编译后的图，用于实际执行 */
	private CompiledGraph compiledGraph;

	/** LLM 调用前的钩子函数 */
	private NodeAction preLlmHook;

	/** LLM 调用后的钩子函数 */
	private NodeAction postLlmHook;

	/** 工具调用前的钩子函数 */
	private NodeAction preToolHook;

	/** 工具调用后的钩子函数 */
	private NodeAction postToolHook;

	/** 可用工具列表 */
	private List<String> tools;

	/** 最大迭代次数，防止无限循环 */
	private int max_iterations = 10;

	/** 当前迭代次数 */
	private int iterations = 0;

	/** 编译配置 */
	private CompileConfig compileConfig;

	/** 状态键策略工厂 */
	private KeyStrategyFactory keyStrategyFactory;

	/** 智能体指令 */
	private String instruction;

	/** 判断是否继续执行的函数 */
	private Function<OverAllState, Boolean> shouldContinueFunc;

	/** 输入键名 */
	private String inputKey = "messages";

	/**
	 * 构造函数，初始化 ReactAgent
	 * @param llmNode LLM 节点
	 * @param toolNode 工具节点
	 * @param builder 构建器
	 * @throws GraphStateException 图状态异常
	 */
	public ReactAgent(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		this.name = builder.name;
		this.description = builder.description;
		this.instruction = builder.instruction;
		this.outputKey = builder.outputKey;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.compileConfig = builder.compileConfig;
		this.shouldContinueFunc = builder.shouldContinueFunc;
		this.preLlmHook = builder.preLlmHook;
		this.postLlmHook = builder.postLlmHook;
		this.preToolHook = builder.preToolHook;
		this.postToolHook = builder.postToolHook;
		this.inputKey = builder.inputKey;

		// 初始化graph
		this.graph = initGraph();
	}

	/**
	 * 同步调用智能体
	 * @param input 输入参数
	 * @return 执行结果的整体状态
	 * @throws GraphStateException 图状态异常
	 * @throws GraphRunnerException 图运行异常
	 */
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.invoke(input);
	}

	/**
	 * 流式调用智能体
	 * @param input 输入参数
	 * @return 异步节点输出生成器
	 * @throws GraphStateException 图状态异常
	 * @throws GraphRunnerException 图运行异常
	 */
	@Override
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.stream(input);
	}


	/**
	 * 获取状态图
	 * @return 状态图实例
	 */
	public StateGraph getStateGraph() {
		return graph;
	}

	/**
	 * 获取编译后的图
	 * @return 编译图实例
	 * @throws GraphStateException 图状态异常
	 */
	public CompiledGraph getCompiledGraph() throws GraphStateException {
		return compiledGraph;
	}

	/**
	 * 使用指定配置编译图
	 * @param compileConfig 编译配置
	 * @return 编译后的图
	 * @throws GraphStateException 图状态异常
	 */
	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = getStateGraph().compile();
		}
		else {
			this.compiledGraph = getStateGraph().compile(compileConfig);
		}
		this.compiledGraph = getStateGraph().compile(compileConfig);
		return this.compiledGraph;
	}

	/**
	 * 编译图并返回
	 * @return 编译后的图
	 * @throws GraphStateException 图状态异常
	 */
	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = getStateGraph().compile();
		}
		else {
			this.compiledGraph = getStateGraph().compile(this.compileConfig);
		}
		return this.compiledGraph;
	}

	/**
	 * 将智能体转换为同步节点操作
	 * @param inputKeyFromParent 从父节点获取输入的键
	 * @param outputKeyToParent 输出到父节点的键
	 * @return 节点操作实例
	 * @throws GraphStateException 图状态异常
	 */
	public NodeAction asNodeAction(String inputKeyFromParent, String outputKeyToParent) throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph);
	}

	/**
	 * 将智能体转换为异步节点操作
	 * @param inputKeyFromParent 从父节点获取输入的键
	 * @param outputKeyToParent 输出到父节点的键
	 * @return 异步节点操作实例
	 * @throws GraphStateException 图状态异常
	 */
	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return node_async(new SubGraphStreamingNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
	}

	/**
	 * 初始化状态图，构建智能体的执行流程
	 * @return 初始化后的状态图
	 * @throws GraphStateException 图状态异常
	 */
	private StateGraph initGraph() throws GraphStateException {
		// 构建messages的Key更新策略
		if (this.keyStrategyFactory == null) {
			this.keyStrategyFactory = KeyStrategyFactoryBuilder.builder()
					.addStrategy("messages", KeyStrategy.APPEND)
					.build();
		} else {
			KeyStrategyFactory originalFactory = this.keyStrategyFactory;
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>(originalFactory.apply());
				keyStrategyHashMap.put("messages", KeyStrategy.APPEND);
				return keyStrategyHashMap;
			};
		}

		// preLlmHook => 更新LlmNode的messages
		NodeAction effectivePreLlmHook = this.preLlmHook;
		if (effectivePreLlmHook == null) {
			effectivePreLlmHook = state -> {
				if (state.value("messages").isPresent()) {
					System.out.println("preLLMNode execute");
//					List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
//					state.updateState(Map.of(this.inputKey, messages));
				}
				return Map.of();
			};
		}

		StateGraph graph = new StateGraph(name, this.keyStrategyFactory);

		graph.addNode("preLlm", node_async(effectivePreLlmHook));
		graph.addNode("llm", node_async(this.llmNode));
		if (postLlmHook != null) {
			graph.addNode("postLlm", node_async(this.postLlmHook));
		}

		if (preToolHook != null) {
			graph.addNode("preTool", node_async(this.preToolHook));
		}

		graph.addNode("tool", node_async(this.toolNode));

		if (postToolHook != null) {
			graph.addNode("postTool", node_async(this.postToolHook));
		}

		graph.addEdge(START, "preLlm").addEdge("preLlm", "llm");

		if (postLlmHook != null) {
			graph.addEdge("llm", "postLlm")
				.addConditionalEdges("postLlm", edge_async(this::think),
						Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}
		else {
			graph.addConditionalEdges("llm", edge_async(this::think),
					Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}

		// 添加工具相关的边
		if (preToolHook != null) {
			graph.addEdge("preTool", "tool");
		}
		if (postToolHook != null) {
			graph.addEdge("tool", "postTool").addEdge("postTool", "preLlm");
		}
		else {
			graph.addEdge("tool", "preLlm");
		}

		return graph;
	}

	/**
	 * 判断是否继续执行的决策函数
	 * @param state 当前整体状态
	 * @return "continue" 表示继续执行工具调用，"end" 表示结束
	 */
	private String think(OverAllState state) {
		if (iterations > max_iterations) {
			return "end";
		}

		if (shouldContinueFunc != null && !shouldContinueFunc.apply(state)) {
			return "end";
		}

		if (state.value("messages").isPresent() && state.value("messages").get() instanceof Message message) {
			state.updateState(Map.of(this.inputKey, List.of(message)));
		}

		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();

		AssistantMessage message = (AssistantMessage) messages.get(messages.size() - 1);
		if (message.hasToolCalls()) {
			return "continue";
		}

		return "end";
	}

	/**
	 * 获取智能体指令
	 * @return 指令内容
	 */
	public String instruction() {
		return instruction;
	}

	/**
	 * 获取智能体的唯一名称。
	 * @return 智能体的唯一名称。
	 */
	public String name() {
		return name;
	}

	/**
	 * 获取智能体能力的一行描述。
	 * @return 智能体的描述。
	 */
	public String description() {
		return description;
	}

	/**
	 * 创建 ReactAgent 构建器
	 * @return Builder 实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * ReactAgent 的构建器类
	 * 支持链式调用，一次 build() 完成所有构建
	 */
	public static class Builder {

		// 必需参数
		private String name;
		private String modelName;
		private String instruction;

		// 可选参数 - 基本配置
		private String description;
		private String outputKey;
		private String inputKey = "messages";
		private int maxIterations = 10;
		private Boolean stream = true;

		// 可选参数 - 工具相关
		private List<ToolCallback> tools;
		private ToolCallbackResolver resolver;

		// 可选参数 - 高级配置
		private CompileConfig compileConfig;
		private KeyStrategyFactory keyStrategyFactory;
		private Function<OverAllState, Boolean> shouldContinueFunc;

		// 可选参数 - 钩子函数
		private NodeAction preLlmHook;
		private NodeAction postLlmHook;
		private NodeAction preToolHook;
		private NodeAction postToolHook;

		// 可选参数 - 外部依赖（如果提供，则不自动创建）
		private ChatClient chatClient;
		private ChatModel chatModel;
		private LlmNode llmNode;
		private ToolNode toolNode;

		private Builder() {
		}

		// ==================== 必需参数 ====================

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		public Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		// ==================== 可选参数 - 基本配置 ====================

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

		public Builder maxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
			return this;
		}

		public Builder stream(boolean stream) {
			this.stream = stream;
			return this;
		}

		// ==================== 可选参数 - 工具相关 ====================

		public Builder tools(List<ToolCallback> tools) {
			this.tools = tools;
			return this;
		}

		public Builder resolver(ToolCallbackResolver resolver) {
			this.resolver = resolver;
			return this;
		}

		// ==================== 可选参数 - 高级配置 ====================

		public Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public Builder keyStrategyFactory(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
			return this;
		}

		public Builder shouldContinueFunc(Function<OverAllState, Boolean> shouldContinueFunc) {
			this.shouldContinueFunc = shouldContinueFunc;
			return this;
		}

		// ==================== 可选参数 - 钩子函数 ====================

		public Builder preLlmHook(NodeAction preLlmHook) {
			this.preLlmHook = preLlmHook;
			return this;
		}

		public Builder postLlmHook(NodeAction postLlmHook) {
			this.postLlmHook = postLlmHook;
			return this;
		}

		public Builder preToolHook(NodeAction preToolHook) {
			this.preToolHook = preToolHook;
			return this;
		}

		public Builder postToolHook(NodeAction postToolHook) {
			this.postToolHook = postToolHook;
			return this;
		}

		// ==================== 可选参数 - 外部依赖 ====================

		/**
		 * 提供外部创建的 ChatClient
		 * 如果提供，将使用此 ChatClient 而不是从 ChatModel 创建
		 */
		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		/**
		 * 提供 ChatModel，Builder 将自动创建 ChatClient
		 * 如果已提供 chatClient，此参数将被忽略
		 */
		public Builder chatModel(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		/**
		 * 提供外部创建的 LlmNode
		 * 如果提供，将使用此 LlmNode 而不是自动创建
		 */
		public Builder llmNode(LlmNode llmNode) {
			this.llmNode = llmNode;
			return this;
		}

		/**
		 * 提供外部创建的 ToolNode
		 * 如果提供，将使用此 ToolNode 而不是自动创建
		 */
		public Builder toolNode(ToolNode toolNode) {
			this.toolNode = toolNode;
			return this;
		}

		// ==================== 构建方法 ====================

		/**
		 * 构建 ReactAgent 实例
		 * @return ReactAgent 实例
		 * @throws GraphStateException 图状态异常
		 * @throws IllegalStateException 如果必需参数未设置
		 */
		public ReactAgent build() throws GraphStateException {
			// 验证必需参数
			validateRequiredParameters();

			// 创建或使用提供的 LlmNode
			LlmNode finalLlmNode = this.llmNode != null ? this.llmNode : createLlmNode();

			// 创建或使用提供的 ToolNode
			ToolNode finalToolNode = this.toolNode != null ? this.toolNode : createToolNode();

			// 创建内部 Builder 对象（用于传递给 ReactAgent 构造函数）
			Builder internalBuilder = new Builder();
			internalBuilder.name = this.name;
			internalBuilder.description = this.description;
			internalBuilder.instruction = this.instruction;
			internalBuilder.outputKey = this.outputKey;
			internalBuilder.inputKey = this.inputKey;
			internalBuilder.maxIterations = this.maxIterations;
			internalBuilder.compileConfig = this.compileConfig;
			internalBuilder.keyStrategyFactory = this.keyStrategyFactory;
			internalBuilder.shouldContinueFunc = this.shouldContinueFunc;
			internalBuilder.preLlmHook = this.preLlmHook;
			internalBuilder.postLlmHook = this.postLlmHook;
			internalBuilder.preToolHook = this.preToolHook;
			internalBuilder.postToolHook = this.postToolHook;

			return new ReactAgent(finalLlmNode, finalToolNode, internalBuilder);
		}

		/**
		 * 验证必需参数
		 */
		private void validateRequiredParameters() {
			// 如果没有提供外部的 LlmNode，则需要验证创建 LlmNode 所需的参数
			if (this.llmNode == null) {
				if (this.name == null || this.name.trim().isEmpty()) {
					throw new IllegalStateException("name is required");
				}
				if (this.instruction == null || this.instruction.trim().isEmpty()) {
					throw new IllegalStateException("instruction is required");
				}
				if (this.chatClient == null && this.chatModel == null) {
					throw new IllegalStateException("Either chatClient or chatModel must be provided");
				}
			}
		}

		/**
		 * 创建 LlmNode
		 */
		private LlmNode createLlmNode() {
			// 获取或创建 ChatClient
			ChatClient finalChatClient = this.chatClient != null ? this.chatClient : createChatClient();

			return LlmNode.builder()
					.systemPrompt(this.instruction)
					.chatClient(finalChatClient)
					.toolCallbacks(this.tools)
					.model(this.modelName)
					.messagesKey(this.inputKey)
					.stream(this.stream)
					.build();
		}

		/**
		 * 从 ChatModel 创建 ChatClient
		 */
		private ChatClient createChatClient() {
			if (this.chatModel == null) {
				throw new IllegalStateException("chatModel is required when chatClient is not provided");
			}

			return ChatClient.builder(this.chatModel)
					.defaultAdvisors(new SimpleLoggerAdvisor())
					.defaultOptions(
							OpenAiChatOptions.builder()
									.internalToolExecutionEnabled(false)
									.parallelToolCalls(false)
									.model(this.modelName)
									.build()
					)
					.build();
		}

		/**
		 * 创建 ToolNode
		 */
		private ToolNode createToolNode() {
			return ToolNode.builder()
					.llmResponseKey(LlmNode.LLM_RESPONSE_KEY)
					.toolCallbackResolver(this.resolver)
					.toolCallbacks(this.tools)
					.build();
		}
	}
	/**
	 * 子图节点适配器，用于将子图封装为同步节点操作。
	 */
	public static class SubGraphNodeAdapter implements NodeAction {

		private final String inputKeyFromParent;

		private final String outputKeyToParent;

		private final CompiledGraph childGraph;

		public SubGraphNodeAdapter(String inputKeyFromParent, String outputKeyToParent, CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		/**
		 * 执行子图节点操作
		 * @param parentState 父状态
		 * @return 更新后的状态映射
		 * @throws Exception 执行异常
		 */
		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {

			// 为子图准备输入
			String input = (String) parentState.value(inputKeyFromParent).orElseThrow();
			Message message = new UserMessage(input);
			List<Message> messages = List.of(message);

			// 调用子图
			OverAllState childState = childGraph.invoke(Map.of("messages", messages)).get();

			// 从子图提取输出
			List<Message> reactMessages = (List<Message>) childState.value("messages").orElseThrow();
			AssistantMessage assistantMessage = (AssistantMessage) reactMessages.get(reactMessages.size() - 1);
			String reactResult = assistantMessage.getText();

			// 更新父状态
			return Map.of(outputKeyToParent, reactResult);
		}

	}

	/**
	 * 子图流式节点适配器，用于将子图封装为异步流式节点操作。
	 */
	public static class SubGraphStreamingNodeAdapter implements NodeAction {

		private final String inputKeyFromParent;

		private final String outputKeyToParent;

		private final CompiledGraph childGraph;

		public SubGraphStreamingNodeAdapter(String inputKeyFromParent, String outputKeyToParent,
				CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		/**
		 * 执行子图流式节点操作
		 * @param parentState 父状态
		 * @return 包含异步生成器的状态映射
		 * @throws Exception 执行异常
		 */
		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {
			Object input = parentState.value(inputKeyFromParent).orElseThrow();
			List<Message> messages;
			if (input instanceof List<?> ms) {
				messages = (List<Message>) ms;
			} else {
				Message message = new UserMessage((String) input);
			 	messages = List.of(message);
			}

			AsyncGenerator<NodeOutput> child = childGraph.stream(Map.of("messages", messages));

			AsyncGenerator<NodeOutput> wrapped = new AsyncGenerator<>() {
				private volatile Map<String, Object> lastStateData;

				@Override
				public Data<NodeOutput> next() {
					Data<NodeOutput> data = child.next();
					if (data.isDone()) {
						String result = extractAssistantText(lastStateData);
						return Data.done(Map.of(outputKeyToParent, result));
					}
					if (data.isError()) {
						return data;
					}
					return Data.of(data.getData().thenApply(n -> {
						try {
							lastStateData = n.getState().data();
						}
						catch (Exception ignored) {
						}
						return n;
					}));
				}
			};

			return Map.of(outputKeyToParent, wrapped);
		}

		/**
		 * 从状态数据中提取助手回复文本
		 * @param stateData 状态数据
		 * @return 助手回复文本，如果无法提取则返回空字符串
		 */
		private String extractAssistantText(Map<String, Object> stateData) {
			if (stateData == null) {
				return "";
			}
			Object msgs = stateData.get("messages");
			if (!(msgs instanceof List)) {
				return "";
			}
			List<?> list = (List<?>) msgs;
			if (list.isEmpty()) {
				return "";
			}
			Object last = list.get(list.size() - 1);
			if (last instanceof AssistantMessage assistant) {
				return assistant.getText();
			}
			return "";
		}

	}

}
