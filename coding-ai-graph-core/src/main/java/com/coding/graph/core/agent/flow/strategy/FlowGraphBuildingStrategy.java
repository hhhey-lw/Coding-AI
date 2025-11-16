package com.coding.graph.core.agent.flow.strategy;


import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;

/**
 * 流图构建策略接口，用于为不同类型的FlowAgent构建StateGraph。
 * 此设计允许在不修改现有代码的情况下扩展图的构建功能。
 *
 * <p>
 * 每种FlowAgent类型都应有自己的策略实现，该实现知道如何构建适当的图结构。
 * </p>
 */
public interface FlowGraphBuildingStrategy {

	/**
	 * 根据提供的配置构建StateGraph。
	 * @param config 包含所有必要参数的图配置
	 * @return 构建完成的StateGraph
	 * @throws GraphStateException 如果图构建失败
	 */
	StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException;

	/**
	 * 返回此策略的类型标识符。用于注册和查找。
	 * @return 策略类型标识符
	 */
	String getStrategyType();

	/**
	 * 验证配置是否包含此策略所需的所有参数。
	 * @param config 要验证的配置
	 * @throws IllegalArgumentException 如果验证失败
	 */
	default void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("配置不能为空");
		}
		if (config.getName() == null || config.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("必须提供图名称");
		}
		if (config.getKeyStrategyFactory() == null) {
			throw new IllegalArgumentException("必须提供KeyStrategyFactory");
		}
		if (config.getRootAgent() == null) {
			throw new IllegalArgumentException("必须提供根代理");
		}
	}

}