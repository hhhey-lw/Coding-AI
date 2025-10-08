package com.coding.graph.core.node.action;

import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

import java.util.Map;

/**
 * 节点行为接口，带有可运行配置
 * 接收全局状态模型和可运行配置，返回一个处理结果
 */
@FunctionalInterface
public interface NodeActionWithConfig {

    Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception;

}
