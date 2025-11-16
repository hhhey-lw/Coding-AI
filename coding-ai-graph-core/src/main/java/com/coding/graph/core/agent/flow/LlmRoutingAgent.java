package com.coding.graph.core.agent.flow;


import com.coding.graph.core.agent.flow.builder.FlowAgentBuilder;
import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.agent.flow.enums.FlowAgentEnum;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.state.OverAllState;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Map;
import java.util.Optional;

/**
 * LlmRoutingAgent 是一种基于大语言模型（LLM）的路由智能体，继承自 FlowAgent。
 * 它利用指定的 ChatModel 来进行路由决策，从而动态选择子智能体执行路径。
 *
 * 设计目标：通过集成 LLM，LlmRoutingAgent 能够根据输入内容智能地决定
 * 最合适的子智能体进行处理，适用于需要复杂决策和多路径选择的 AI 任务场景。
 */
public class LlmRoutingAgent extends FlowAgent {

	private final ChatModel chatModel;

	protected LlmRoutingAgent(LlmRoutingAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.chatModel = builder.chatModel;
		this.graph = initGraph();
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.invoke(input);
	}

	@Override
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.stream(input);
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		config.setChatModel(this.chatModel);
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.ROUTING.getType(), config);
	}

	public static LlmRoutingAgentBuilder builder() {
		return new LlmRoutingAgentBuilder();
	}

	/**
	 * 用于创建 LlmRoutingAgent 实例的构建器。
	 * 继承通用的 FlowAgentBuilder 并添加 LLM 特定的配置。
	 */
	public static class LlmRoutingAgentBuilder extends FlowAgentBuilder<LlmRoutingAgent, LlmRoutingAgentBuilder> {

		private ChatModel chatModel;

		/**
		 * 设置用于基于 LLM 路由决策的 ChatModel。
		 * @param chatModel 用于路由的聊天模型
		 * @return 用于方法链式调用的构建器实例
		 */
		public LlmRoutingAgentBuilder model(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		@Override
		protected LlmRoutingAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			if (chatModel == null) {
				throw new IllegalArgumentException("ChatModel must be provided for LLM routing agent");
			}
		}

		@Override
		public LlmRoutingAgent build() throws GraphStateException {
			validate();
			return new LlmRoutingAgent(this);
		}

	}

}
