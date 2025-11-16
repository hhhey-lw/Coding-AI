package com.coding.graph.core.agent.flow.strategy;


import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.flow.FlowAgent;
import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.agent.flow.enums.FlowAgentEnum;
import com.coding.graph.core.agent.flow.node.ConditionEvaluatorAction;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.agent.flow.node.ConditionEvaluator;
import com.coding.graph.core.agent.flow.node.TransparentNode;

import java.util.HashMap;
import java.util.Map;

import static com.coding.graph.core.common.NodeCodeConstants.END;
import static com.coding.graph.core.common.NodeCodeConstants.START;
import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;;

/**
 * 条件执行图构建策略类。在条件图中，执行路径通过根据当前状态评估条件来确定，
 * 并据此选择执行不同的智能体。
 *
 * 核心逻辑：先通过根节点完成初始化，再通过条件评估节点判断执行条件，
 * 最后根据条件匹配结果路由到对应的条件智能体，实现"分支式"流程控制。
 */
public class ConditionalGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	/**
	 * 构建条件执行图
	 * 步骤：1. 校验配置合法性 2. 初始化状态图 3. 添加根透明节点 4. 添加条件评估节点
	 *      5. 注册条件智能体节点 6. 配置条件路由规则 7. 返回构建完成的状态图
	 *
	 * @param config 图构建配置，包含图名称、键策略工厂、根智能体、条件智能体等参数
	 * @return 构建完成的条件执行状态图（StateGraph）
	 * @throws GraphStateException 图构建过程中出现异常（如配置无效、节点添加失败）
	 */
	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		// 校验基础配置（如名称、根智能体、键策略工厂）
		validateConfig(config);
		// 校验条件相关专属配置（如条件智能体非空）
		validateConditionalConfig(config);

		// 初始化状态图：传入图名称和键策略工厂
		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		// 获取配置中的根智能体（需为FlowAgent类型，用于后续获取输入键）
		BaseAgent rootAgent = config.getRootAgent();

		// 1. 添加根透明节点：透明节点不处理业务逻辑，仅用于数据透传（从根智能体输出键到FlowAgent输入键）
		graph.addNode(
				rootAgent.name(), // 节点名称：使用根智能体的名称，确保唯一性
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey()))
		);

		// 2. 添加起始边：从状态图的"开始节点"（START）指向根透明节点
		graph.addEdge(START, rootAgent.name());

		// 3. 添加条件评估节点：用于评估当前状态，确定后续执行的条件智能体
		String conditionNodeName = rootAgent.name() + "_condition"; // 节点名称：根智能体名称+_condition，确保唯一
		graph.addNode(conditionNodeName, node_async(new ConditionEvaluator()));
		// 连接根透明节点到条件评估节点：根节点执行完成后进入条件判断
		graph.addEdge(rootAgent.name(), conditionNodeName);

		// 4. 处理条件智能体：遍历配置中的条件智能体映射，注册为图节点并配置路由
		Map<String, String> conditionRoutingMap = new HashMap<>(); // 条件路由映射：条件key→智能体节点名称
		for (Map.Entry<String, BaseAgent> entry : config.getConditionalAgents().entrySet()) {
			String condition = entry.getKey(); // 条件标识（如"conditionA"、"conditionB"）
			BaseAgent agent = entry.getValue(); // 该条件对应的智能体

			// 4.1 注册条件智能体为图节点：将智能体转换为异步节点动作，指定数据输入输出键
			graph.addNode(
					agent.name(), // 节点名称：使用智能体自身名称
					agent.asAsyncNodeAction(rootAgent.outputKey(), agent.outputKey()) // 输入键：根智能体输出；输出键：当前智能体输出
			);

			// 4.2 记录条件与智能体节点的映射关系：用于后续条件路由
			conditionRoutingMap.put(condition, agent.name());

			// 4.3 连接条件智能体节点到"结束节点"（END）：智能体执行完成后流程结束
			graph.addEdge(agent.name(), END);
		}

		// 5. 添加默认路由规则：若所有条件均不匹配，直接指向"结束节点"（END）
		conditionRoutingMap.put("default", END);

		// 6. 为条件评估节点配置条件路由边：根据条件评估结果，路由到对应的节点
		graph.addConditionalEdges(
				conditionNodeName,          // 源节点：条件评估节点
				new ConditionEvaluatorAction(), // 条件评估动作：定义如何从状态中获取条件结果
				conditionRoutingMap         // 条件路由映射：条件→目标节点
		);

		// 返回构建完成的条件执行图
		return graph;
	}

	/**
	 * 获取当前策略的类型标识
	 * 与FlowAgentEnum.CONDITIONAL枚举的类型值一致，用于策略注册和查找
	 *
	 * @return 策略类型字符串（如"conditional"）
	 */
	@Override
	public String getStrategyType() {
		return FlowAgentEnum.CONDITIONAL.getType();
	}

	/**
	 * 校验图构建的基础配置合法性
	 * 先调用父接口的基础校验（如名称、根智能体、键策略工厂非空），再补充条件专属校验
	 *
	 * @param config 图构建配置
	 * @throws IllegalArgumentException 配置不满足要求时抛出（如条件智能体为空）
	 */
	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		// 调用父接口的基础校验逻辑（来自FlowGraphBuildingStrategy）
		FlowGraphBuildingStrategy.super.validateConfig(config);
		// 补充条件流程专属的配置校验
		validateConditionalConfig(config);
	}

	/**
	 * 校验条件流程专属的配置要求
	 * 确保条件智能体非空、根智能体为FlowAgent类型、条件key非空
	 *
	 * @param config 图构建配置
	 * @throws IllegalArgumentException 配置不满足条件流程要求时抛出
	 */
	private void validateConditionalConfig(FlowGraphBuilder.FlowGraphConfig config) {
		// 1. 校验条件智能体映射非空：条件流程至少需要一个条件智能体
		if (config.getConditionalAgents() == null || config.getConditionalAgents().isEmpty()) {
			throw new IllegalArgumentException("条件流程至少需要一个条件智能体映射（Conditional flow requires at least one conditional agent mapping）");
		}

		// 2. 校验根智能体类型为FlowAgent：需通过FlowAgent获取输入键，用于透明节点数据透传
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("条件流程要求根智能体必须是FlowAgent类型（Conditional flow requires root agent to be a FlowAgent）");
		}

		// 3. 校验所有条件key非空：条件标识不能为null或空白字符串
		for (String condition : config.getConditionalAgents().keySet()) {
			if (condition == null || condition.trim().isEmpty()) {
				throw new IllegalArgumentException("条件key不能为null或空白字符串（Condition keys cannot be null or empty）");
			}
		}
	}

}