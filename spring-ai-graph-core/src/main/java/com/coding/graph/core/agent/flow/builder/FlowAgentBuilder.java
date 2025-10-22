package com.coding.graph.core.agent.flow.builder;

import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.flow.FlowAgent;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;

import java.util.List;

/**
 * FlowAgent 及其子类的抽象基础构建器。提供通用的构建器功能并强制执行一致的构建器模式。
 *
 * @param <T> 此构建器创建的具体 FlowAgent 类型
 * @param <B> 具体的构建器类型（用于支持流式接口）
 */
public abstract class FlowAgentBuilder<T extends FlowAgent, B extends FlowAgentBuilder<T, B>> {

    // FlowAgent 的通用属性
    public String name;

    public String description;

    public String outputKey;

    public String inputKey;

    public KeyStrategyFactory keyStrategyFactory;

    public CompileConfig compileConfig;

    public List<BaseAgent> subAgents;

    /**
     * 设置代理名称。
     * @param name 代理的唯一名称
     * @return 用于方法链式调用的构建器实例
     */
    public B name(String name) {
        this.name = name;
        return self();
    }

    /**
     * 设置代理描述。
     * @param description 代理能力的描述
     * @return 用于方法链式调用的构建器实例
     */
    public B description(String description) {
        this.description = description;
        return self();
    }

    /**
     * 设置代理结果的输出键。
     * @param outputKey 输出键
     * @return 用于方法链式调用的构建器实例
     */
    public B outputKey(String outputKey) {
        this.outputKey = outputKey;
        return self();
    }

    /**
     * 设置代理的输入键。
     * @param inputKey 输入键
     * @return 用于方法链式调用的构建器实例
     */
    public B inputKey(String inputKey) {
        this.inputKey = inputKey;
        return self();
    }

    /**
     * 设置状态管理的键策略工厂。
     * @param keyStrategyFactory 键策略工厂
     * @return 用于方法链式调用的构建器实例
     */
    public B state(KeyStrategyFactory keyStrategyFactory) {
        this.keyStrategyFactory = keyStrategyFactory;
        return self();
    }

    /**
     * 设置编译配置。
     * @param compileConfig 编译配置
     * @return 用于方法链式调用的构建器实例
     */
    public B compileConfig(CompileConfig compileConfig) {
        this.compileConfig = compileConfig;
        return self();
    }

    /**
     * 设置子代理列表。
     * @param subAgents 子代理列表
     * @return 用于方法链式调用的构建器实例
     */
    public B subAgents(List<BaseAgent> subAgents) {
        this.subAgents = subAgents;
        return self();
    }

    /**
     * 返回具体的构建器实例。此方法在子类中支持流式接口。
     * @return 此构建器实例
     */
    protected abstract B self();

    /**
     * 在创建代理之前验证构建器状态。子类可以重写此方法以添加特定的验证逻辑。
     * @throws IllegalArgumentException 如果验证失败
     */
    protected void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must be provided");
        }
        if (subAgents == null || subAgents.isEmpty()) {
            throw new IllegalArgumentException("At least one sub-agent must be provided for flow");
        }
    }

    /**
     * 构建具体的 FlowAgent 实例。子类必须实现此方法以创建特定的代理类型。
     * @return 构建的 FlowAgent 实例
     * @throws GraphStateException 如果代理创建失败
     */
    public abstract T build() throws GraphStateException;

}
