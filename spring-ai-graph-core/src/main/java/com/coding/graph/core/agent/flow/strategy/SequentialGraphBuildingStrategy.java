package com.coding.graph.core.agent.flow.strategy;


import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.flow.FlowAgent;
import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.agent.flow.enums.FlowAgentEnum;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.agent.flow.node.TransparentNode;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;

/**
 * 用于构建顺序执行图的策略。在顺序图中，代理以线性链的形式连接，每个代理的输出成为下一个代理的输入。
 */
public class SequentialGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateSequentialConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// 添加根透明节点
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// 添加起始边
		graph.addEdge(START, rootAgent.name());

		// 按顺序处理子代理
		BaseAgent currentAgent = rootAgent;
		for (BaseAgent subAgent : config.getSubAgents()) {
			// 添加当前子代理作为节点
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(currentAgent.outputKey(), subAgent.outputKey()));
			graph.addEdge(currentAgent.name(), subAgent.name());
			currentAgent = subAgent;
		}

		// 将最后一个代理连接到END
		graph.addEdge(currentAgent.name(), END);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.SEQUENTIAL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
		validateSequentialConfig(config);
	}

	/**
	 * 验证顺序执行特定的配置要求。
	 * @param config 要验证的配置
	 * @throws IllegalArgumentException 如果验证失败
	 */
	private void validateSequentialConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("顺序流程至少需要一个子代理");
		}

		// 确保根代理是FlowAgent以访问输入键
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("顺序流程要求根代理必须是FlowAgent");
		}
	}

}