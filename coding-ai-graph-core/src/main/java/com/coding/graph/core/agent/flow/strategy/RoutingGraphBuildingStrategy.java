package com.coding.graph.core.agent.flow.strategy;


import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.flow.FlowAgent;
import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.agent.flow.enums.FlowAgentEnum;
import com.coding.graph.core.agent.flow.node.RoutingEdgeAction;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.agent.flow.node.TransparentNode;

import java.util.HashMap;
import java.util.Map;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;


/**
 * 基于LLM的路由图构建策略。在路由图中，LLM会根据输入内容和代理能力决定应由哪个子代理处理任务。
 */
public class RoutingGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateRoutingConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// 添加根透明节点
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// 添加起始边到透明节点
		graph.addEdge(START, rootAgent.name());

		// 处理用于路由的子代理
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (BaseAgent subAgent : config.getSubAgents()) {
			// 添加当前子代理作为节点
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(rootAgent.outputKey(), subAgent.outputKey()));
			edgeRoutingMap.put(subAgent.name(), subAgent.name());

			// 将子代理连接到END（除非它们是带有自己子代理的FlowAgent）
			if (subAgent instanceof FlowAgent subFlowAgent) {
				if (subFlowAgent.subAgents() == null || subFlowAgent.subAgents().isEmpty()) {
					graph.addEdge(subAgent.name(), END);
				}
			}
			else {
				graph.addEdge(subAgent.name(), END);
			}
		}

		// 通过条件路由将父节点连接到子代理
		graph.addConditionalEdges(rootAgent.name(),
				new RoutingEdgeAction(config.getChatModel(), rootAgent, config.getSubAgents()), edgeRoutingMap);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.ROUTING.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
		validateRoutingConfig(config);
	}

	/**
	 * 验证路由特定的配置要求。
	 * @param config 要验证的配置
	 * @throws IllegalArgumentException 如果验证失败
	 */
	private void validateRoutingConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("路由流程至少需要一个子代理");
		}

		if (config.getChatModel() == null) {
			throw new IllegalArgumentException("路由流程需要ChatModel来进行决策");
		}

		// 确保根代理是FlowAgent以访问输入键
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("路由流程要求根代理必须是FlowAgent");
		}
	}

}