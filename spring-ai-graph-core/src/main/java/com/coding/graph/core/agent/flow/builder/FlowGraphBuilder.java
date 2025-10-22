package com.coding.graph.core.agent.flow.builder;

import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.flow.strategy.FlowGraphBuildingStrategy;
import com.coding.graph.core.agent.flow.strategy.FlowGraphBuildingStrategyRegistry;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FlowGraphBuilder是一个集中式的状态图构建器，用于为不同类型的FlowAgent构建StateGraph实例。
 *
 * 该类采用策略模式，通过委托给具体的FlowGraphBuildingStrategy实现类来完成图的构建，
 * 从而将图的构建逻辑与具体的智能体类型解耦，提高了代码的可复用性和可维护性。
 *
 * 主要功能包括：
 * 1. 提供静态方法buildGraph()，根据策略类型和配置构建状态图
 * 2. 定义内部类FlowGraphConfig，封装图构建所需的所有配置参数
 * 3. 支持通过策略注册表动态获取构建策略，实现灵活的图构建逻辑扩展
 */
public class FlowGraphBuilder {

	/**
	 * 通用的图构建方法，根据指定的策略类型和配置构建状态图
	 * 该方法会从策略注册表中获取对应的构建策略，并委托其完成图的构建
	 *
	 * @param strategyType 构建策略类型，用于从注册表中获取对应的策略实现
	 * @param config 图构建配置，包含构建图所需的所有参数
	 * @return 构建完成的StateGraph实例
	 * @throws GraphStateException 当图构建失败（如策略不存在、配置无效）时抛出
	 */
	public static StateGraph buildGraph(String strategyType, FlowGraphConfig config) throws GraphStateException {
		// 从策略注册表中获取指定类型的构建策略
		FlowGraphBuildingStrategy strategy = FlowGraphBuildingStrategyRegistry.getInstance().getStrategy(strategyType);
		// 验证配置的有效性
		strategy.validateConfig(config);
		// 委托策略实现类构建状态图
		return strategy.buildGraph(config);
	}

	/**
	 * 图构建配置类，封装了构建StateGraph所需的所有参数
	 * 提供了Builder模式便于配置的创建和修改，支持设置基础属性和自定义属性
	 */
	public static class FlowGraphConfig {

		// 图的名称，用于标识和区分不同的状态图
		private String name;

		// 键策略工厂，用于生成图中节点的键管理策略
		private KeyStrategyFactory keyStrategyFactory;

		// 根智能体，当前图的根节点对应的智能体
		private BaseAgent rootAgent;

		// 子智能体列表，图中包含的子智能体集合
		private List<BaseAgent> subAgents;

		// 条件智能体映射，键为条件标识，值为对应的智能体（用于分支流程）
		private Map<String, BaseAgent> conditionalAgents;

		// 聊天模型，用于图中需要AI交互的节点
		private ChatModel chatModel;

		// 自定义属性映射，用于存储额外的配置参数
		private Map<String, Object> customProperties = new HashMap<>();

		// Getter和Setter方法
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public KeyStrategyFactory getKeyStrategyFactory() {
			return keyStrategyFactory;
		}

		public void setKeyStrategyFactory(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
		}

		public BaseAgent getRootAgent() {
			return rootAgent;
		}

		public void setRootAgent(BaseAgent rootAgent) {
			this.rootAgent = rootAgent;
		}

		public List<BaseAgent> getSubAgents() {
			return subAgents;
		}

		public void setSubAgents(List<BaseAgent> subAgents) {
			this.subAgents = subAgents;
		}

		public Map<String, BaseAgent> getConditionalAgents() {
			return conditionalAgents;
		}

		public void setConditionalAgents(Map<String, BaseAgent> conditionalAgents) {
			this.conditionalAgents = conditionalAgents;
		}

		public ChatModel getChatModel() {
			return chatModel;
		}

		public void setChatModel(ChatModel chatModel) {
			this.chatModel = chatModel;
		}

		// Builder模式方法
		/**
		 * 创建FlowGraphConfig的构建器实例
		 * @return 新的FlowGraphConfig实例
		 */
		public static FlowGraphConfig builder() {
			return new FlowGraphConfig();
		}

		/**
		 * 设置图的名称
		 * @param name 图名称
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * 设置键策略工厂
		 * @param factory 键策略工厂实例
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig keyStrategyFactory(KeyStrategyFactory factory) {
			this.keyStrategyFactory = factory;
			return this;
		}

		/**
		 * 设置根智能体
		 * @param agent 根智能体实例
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig rootAgent(BaseAgent agent) {
			this.rootAgent = agent;
			return this;
		}

		/**
		 * 设置子智能体列表
		 * @param agents 子智能体列表
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig subAgents(List<BaseAgent> agents) {
			this.subAgents = agents;
			return this;
		}

		/**
		 * 设置条件智能体映射
		 * @param agents 条件智能体映射（键为条件标识，值为智能体）
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig conditionalAgents(Map<String, BaseAgent> agents) {
			this.conditionalAgents = agents;
			return this;
		}

		/**
		 * 设置聊天模型
		 * @param model 聊天模型实例
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig chatModel(ChatModel model) {
			this.chatModel = model;
			return this;
		}

		/**
		 * 添加自定义属性
		 * @param key 属性键
		 * @param value 属性值
		 * @return 当前FlowGraphConfig实例（用于链式调用）
		 */
		public FlowGraphConfig customProperty(String key, Object value) {
			this.customProperties.put(key, value);
			return this;
		}

		/**
		 * 获取指定键的自定义属性值
		 * @param key 属性键
		 * @return 属性值（不存在则返回null）
		 */
		public Object getCustomProperty(String key) {
			return this.customProperties.get(key);
		}

		/**
		 * 获取所有自定义属性的不可修改副本
		 * @return 自定义属性映射的不可修改副本
		 */
		public Map<String, Object> getCustomProperties() {
			return Map.copyOf(this.customProperties);
		}

	}

}