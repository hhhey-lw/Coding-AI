package com.coding.graph.core.node.action;

import com.coding.graph.core.state.OverAllState;

import java.util.Map;

/**
 * 节点行为接口
 * 接收全局状态模型，返回一个处理结果
 */
@FunctionalInterface
public interface NodeAction {
    Map<String, Object> apply(OverAllState state) throws Exception;
}
