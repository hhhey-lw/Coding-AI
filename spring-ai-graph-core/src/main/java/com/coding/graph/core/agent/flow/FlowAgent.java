package com.coding.graph.core.agent.flow;

import com.coding.graph.core.agent.BaseAgent;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.agent.flow.builder.FlowGraphBuilder;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.graph.StateGraph;
import com.coding.graph.core.node.config.CompileConfig;
import com.coding.graph.core.state.strategy.KeyStrategyFactory;

import com.coding.graph.core.node.action.AsyncNodeAction;

import java.util.List;

import static com.coding.graph.core.node.action.AsyncNodeAction.node_async;


/**
 * FlowAgent是抽象基类，继承自BaseAgent，核心作用是构建、管理流程化智能体（Agent）的图结构，
 * 支持整合多个子智能体形成协作流程，并提供图的初始化、编译、调度及异步节点转换能力。
 *
 * 核心设计目标：通过标准化的图构建逻辑，让不同类型的流程化智能体（如线性流程、分支流程）
 * 可基于此类扩展，同时兼容智能体的组合与嵌套，适配复杂的AI任务流程场景。
 */
public abstract class FlowAgent extends BaseAgent {

    // 图编译配置：控制StateGraph编译为可执行CompiledGraph的过程（如编译规则、优化策略等）
    protected CompileConfig compileConfig;

    // 输入键：标识当前智能体接收输入数据时，在状态容器中的存储键（用于数据定位）
    protected String inputKey;

    // 键策略工厂：生成图中节点的键管理策略（如节点唯一标识生成、数据键映射规则）
    protected KeyStrategyFactory keyStrategyFactory;

    // 子智能体列表：当前FlowAgent所包含的子智能体集合，用于组合形成多步骤流程
    protected List<BaseAgent> subAgents;

    // 状态图实例：未编译的原始图结构，描述子智能体的协作关系（如节点依赖、执行顺序）
    protected StateGraph graph;

    // 已编译图实例：经过compile()处理后的可执行图，具备调度、运行能力
    protected CompiledGraph compiledGraph;

    /**
     * 构造方法：初始化FlowAgent的核心属性，依赖BaseAgent的基础信息（名称、描述、输出键）
     *
     * @param name 智能体唯一名称（用于日志、监控及节点标识）
     * @param description 智能体功能描述（说明当前流程的业务作用）
     * @param outputKey 输出键：标识当前智能体输出数据在状态容器中的存储键
     * @param inputKey 输入键：标识当前智能体输入数据在状态容器中的存储键
     * @param keyStrategyFactory 键策略工厂：提供图节点的键管理逻辑
     * @param compileConfig 编译配置：控制图的编译过程
     * @param subAgents 子智能体列表：当前流程包含的子智能体
     * @throws GraphStateException 图状态异常（如子智能体为空、配置冲突等）
     */
    protected FlowAgent(String name, String description, String outputKey, String inputKey,
                        KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig, List<BaseAgent> subAgents)
            throws GraphStateException {
        super(name, description, outputKey);
        this.compileConfig = compileConfig;
        this.inputKey = inputKey;
        this.keyStrategyFactory = keyStrategyFactory;
        this.subAgents = subAgents;
    }

    /**
     * 初始化状态图：基于FlowGraphBuilder配置构建原始StateGraph，
     * 具体图结构由子类的buildSpecificGraph()实现（如线性流程、分支流程）
     *
     * @return 构建完成的原始状态图（未编译）
     * @throws GraphStateException 图构建异常（如配置缺失、子智能体非法）
     */
    protected StateGraph initGraph() throws GraphStateException {
        // 构建图配置：整合当前智能体的名称、键策略、子智能体等信息
        FlowGraphBuilder.FlowGraphConfig config = FlowGraphBuilder.FlowGraphConfig.builder()
                .name(this.name())          // 图名称与智能体名称一致
                .keyStrategyFactory(keyStrategyFactory)  // 注入键策略工厂
                .rootAgent(this)            // 标记当前FlowAgent为图的根节点
                .subAgents(this.subAgents()); // 注入子智能体列表

        // 委托子类实现具体图结构（模板方法模式：父类定义流程，子类实现细节）
        return buildSpecificGraph(config);
    }

    /**
     * 抽象方法：子类需实现此方法定义具体的图结构构建逻辑（核心扩展点）
     * 例如：线性流程子类可构建"顺序执行节点链"，分支流程子类可构建"条件判断节点"
     *
     * @param config 图配置：包含构建图所需的基础信息（根节点、子智能体、键策略等）
     * @return 构建完成的StateGraph（原始图结构）
     * @throws GraphStateException 图构建异常（如节点循环依赖、配置不完整）
     */
    protected abstract StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException;

    /**
     * 转换为异步节点动作：将当前FlowAgent包装为AsyncNodeAction，
     * 支持嵌入到其他父图中作为一个异步执行节点（适配智能体嵌套场景）
     *
     * @param inputKeyFromParent 父节点输入键：父图传递数据时使用的存储键
     * @param outputKeyToParent 父节点输出键：当前节点向父图返回数据时使用的存储键
     * @return 异步节点动作：可被父图调度执行的节点实例
     * @throws GraphStateException 图状态异常（如未编译、配置缺失）
     */
    @Override
    public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
            throws GraphStateException {
        // 懒加载编译：若未编译则先执行编译（避免重复编译）
        if (this.compiledGraph == null) {
            this.compiledGraph = getAndCompileGraph();
        }
        // 适配ReactAgent的子图流式节点：实现父子图的数据传递与异步执行
        return node_async(
                new ReactAgent.SubGraphStreamingNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph)
        );
    }

    /**
     * 获取并编译图：懒加载编译逻辑，若未配置compileConfig则使用默认编译规则，
     * 若已配置则按自定义规则编译（确保编译结果唯一）
     *
     * @return 已编译的CompiledGraph（可执行图）
     * @throws GraphStateException 图编译异常（如原始图未初始化、编译规则冲突）
     */
    public CompiledGraph getAndCompileGraph() throws GraphStateException {
        // 若未编译则执行编译（避免重复编译）
        if (this.compiledGraph == null) {
            // 若未初始化原始图，则先初始化
            if (this.graph == null) {
                this.graph = initGraph();
            }
            // 按配置编译：无自定义配置则用默认规则
            if (this.compileConfig == null) {
                this.compiledGraph = graph.compile();
            } else {
                this.compiledGraph = graph.compile(this.compileConfig);
            }
        }
        return this.compiledGraph;
    }

    /**
     * 获取编译配置（对外提供配置查询能力）
     * @return 编译配置实例
     */
    public CompileConfig compileConfig() {
        return compileConfig;
    }

    /**
     * 获取输入键（对外提供输入数据键的查询能力）
     * @return 输入键字符串
     */
    public String inputKey() {
        return inputKey;
    }

    /**
     * 获取键策略工厂（对外提供键策略的查询能力）
     * @return 键策略工厂实例
     */
    public KeyStrategyFactory keyStrategyFactory() {
        return keyStrategyFactory;
    }

    /**
     * 获取子智能体列表（对外提供子智能体的查询能力）
     * @return 子智能体列表（不可修改，需通过构造方法初始化）
     */
    public List<BaseAgent> subAgents() {
        return this.subAgents;
    }

}