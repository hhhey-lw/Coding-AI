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

import java.util.Map;
import java.util.Optional;

public class SequentialAgent extends FlowAgent {

	protected SequentialAgent(SequentialAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
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
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.SEQUENTIAL.getType(), config);
	}

	public static SequentialAgentBuilder builder() {
		return new SequentialAgentBuilder();
	}

	/**
	 * 用于创建 SequentialAgent 实例的构建器。
	 * 继承通用的 FlowAgentBuilder 以提供类型安全的构建。
	 */
	public static class SequentialAgentBuilder extends FlowAgentBuilder<SequentialAgent, SequentialAgentBuilder> {

		@Override
		protected SequentialAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			// 如有需要，在此添加 SequentialAgent 特定的验证逻辑
		}

		@Override
		public SequentialAgent build() throws GraphStateException {
			validate();
			return new SequentialAgent(this);
		}

	}

}
