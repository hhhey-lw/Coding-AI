package com.coding.graph.core.agent.flow.strategy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FlowGraphBuildingStrategyRegistry是FlowGraph构建策略的注册中心，采用单例模式设计，
 * 负责统一管理所有FlowGraphBuildingStrategy（图构建策略）的注册、查询、移除等操作。
 * 
 * 核心价值在于实现“策略与使用方解耦”：支持动态注册新策略，无需修改核心构建逻辑即可扩展图构建能力，
 * 同时保证线程安全，可在运行时安全地新增/移除策略，适配多线程环境下的策略管理需求。
 * 
 * 主要特性：
 * 1. 单例实例：确保全系统策略管理的唯一性，避免多实例导致的策略不一致
 * 2. 线程安全：基于ConcurrentHashMap存储策略，支持多线程并发操作
 * 3. 默认策略：初始化时自动注册常用内置策略（如顺序、路由、并行等）
 * 4. 动态扩展：支持运行时注册新策略，满足定制化图构建场景
 */
public class FlowGraphBuildingStrategyRegistry {

    // 单例实例：类加载时初始化，保证线程安全（饿汉式单例）
    private static final FlowGraphBuildingStrategyRegistry INSTANCE = new FlowGraphBuildingStrategyRegistry();

    // 策略存储容器：key为策略类型（如"sequential"、"parallel"），value为对应的策略实现
    // 使用ConcurrentHashMap确保多线程环境下的安全读写
    private final Map<String, FlowGraphBuildingStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * 私有构造方法：禁止外部实例化，确保单例特性
     * 构造时自动注册默认策略，使系统启动后即可使用常用图构建能力
     */
    private FlowGraphBuildingStrategyRegistry() {
        // 初始化默认策略（如顺序流程、路由流程、并行流程等）
        registerDefaultStrategies();
    }

    /**
     * 获取单例注册中心实例
     * @return 全系统唯一的FlowGraphBuildingStrategyRegistry实例
     */
    public static FlowGraphBuildingStrategyRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册新的图构建策略
     * 注册前会校验策略合法性（非空、类型非空），避免无效策略或重复注册
     * @param strategy 待注册的图构建策略实例
     * @throws IllegalArgumentException 当策略为空、策略类型为空/空白、或策略类型已注册时抛出
     */
    public void registerStrategy(FlowGraphBuildingStrategy strategy) {
        // 校验策略实例非空
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null（策略实例不能为null）");
        }

        // 获取策略类型（如"sequential"），并校验类型合法性
        String type = strategy.getStrategyType();
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy type cannot be null or empty（策略类型不能为null或空白字符串）");
        }

        // 校验策略类型是否已注册，避免重复注册导致的策略覆盖
        if (strategies.containsKey(type)) {
            throw new IllegalArgumentException("Strategy type '" + type + "' is already registered（策略类型'" + type + "'已注册）");
        }

        // 注册策略到容器
        strategies.put(type, strategy);
    }

    /**
     * 根据策略类型查询对应的策略实现
     * @param type 策略类型（如"sequential"表示顺序流程策略）
     * @return 对应的FlowGraphBuildingStrategy实例
     * @throws IllegalArgumentException 当策略类型为空/空白，或无对应策略时抛出
     */
    public FlowGraphBuildingStrategy getStrategy(String type) {
        // 校验策略类型合法性
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy type cannot be null or empty（策略类型不能为null或空白字符串）");
        }

        // 根据类型查询策略
        FlowGraphBuildingStrategy strategy = strategies.get(type);
        // 若未找到对应策略，抛出异常提示
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for type: " + type + "（无类型为'" + type + "'的已注册策略）");
        }

        return strategy;
    }

    /**
     * 检查指定类型的策略是否已注册
     * @param type 待检查的策略类型
     * @return true：策略已注册；false：策略未注册（或类型为null）
     */
    public boolean hasStrategy(String type) {
        // 若类型为null直接返回false，避免空指针异常
        return type != null && strategies.containsKey(type);
    }

    /**
     * 获取所有已注册的策略类型
     * @return 不可修改的策略类型集合（避免外部修改容器内容）
     */
    public Set<String> getRegisteredTypes() {
        // 返回键集合的不可修改副本，防止外部通过集合修改内部存储
        return Set.copyOf(strategies.keySet());
    }

    /**
     * 移除指定类型的策略（主要用于测试场景或动态卸载不需要的策略）
     * @param type 待移除的策略类型
     * @return 被移除的策略实例；若该类型无对应策略，返回null
     */
    public FlowGraphBuildingStrategy unregisterStrategy(String type) {
        // 从容器中移除策略并返回
        return strategies.remove(type);
    }

    /**
     * 清空所有已注册的策略（主要用于测试场景，避免测试用例间的策略干扰）
     */
    public void clear() {
        strategies.clear();
    }

    /**
     * 注册系统默认的图构建策略
     * 系统启动后自动加载这些策略，支持常见的流程场景（顺序、路由、并行、条件、循环）
     */
    private void registerDefaultStrategies() {
        // 1. 顺序流程策略：子智能体按顺序依次执行（如A→B→C）
        registerStrategy(new SequentialGraphBuildingStrategy());
        // 2. 路由流程策略：根据条件选择一个子智能体执行（如分支判断）
        registerStrategy(new RoutingGraphBuildingStrategy());
        // 4. 条件流程策略：根据条件决定是否执行某个子智能体（如if-else逻辑）
        registerStrategy(new ConditionalGraphBuildingStrategy());
    }

}