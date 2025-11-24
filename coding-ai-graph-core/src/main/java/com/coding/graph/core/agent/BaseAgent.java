package com.coding.graph.core.agent;

import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.action.AsyncNodeAction;
import com.coding.graph.core.state.OverAllState;

import java.util.Map;
import java.util.Optional;

/**
 * 图系统中所有智能体的抽象基类。包含不同智能体实现共享的通用属性和方法。
 */
public abstract class BaseAgent {

    /**
     * 智能体的名称。必须是图中的唯一标识符。
     */
    protected String name;

    /**
     * 关于智能体能力的一行描述。系统可以使用此描述在将控制权委派给不同智能体时进行决策。
     */
    protected String description;

    /**
     * 智能体结果的输出键
     */
    protected String outputKey;

    /**
     * 用于初始化所有基础智能体属性的受保护构造函数。
     *
     * @param name        智能体的唯一名称
     * @param description 智能体能力的描述
     * @param outputKey   智能体结果的输出键
     */
    protected BaseAgent(String name, String description, String outputKey) {
        this.name = name;
        this.description = description;
        this.outputKey = outputKey;
    }

    /**
     * 默认的受保护构造函数，供需要以不同方式初始化属性的子类使用。
     */
    protected BaseAgent() {
        // 允许子类通过其他方式初始化属性
    }

    /**
     * 获取智能体的唯一名称。
     *
     * @return 智能体的唯一名称。
     */
    public String name() {
        return name;
    }

    /**
     * 获取智能体能力的一行描述。
     *
     * @return 智能体的描述。
     */
    public String description() {
        return description;
    }

    /**
     * 获取智能体结果的输出键。
     *
     * @return 输出键。
     */
    public String outputKey() {
        return outputKey;
    }

    /**
     * 将复杂的智能体抽象为图中的简单节点。
     *
     * @return 子智能体列表。
     */
    public abstract AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
            throws GraphStateException;

    public abstract Optional<OverAllState> invoke(Map<String, Object> input)
            throws GraphStateException, GraphRunnerException;

    public abstract AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
            throws GraphStateException, GraphRunnerException;

}
