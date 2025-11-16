package com.coding.graph.core.node.action;

import com.coding.graph.core.state.OverAllState;

/**
 * 边动作接口，定义了一个接受 OverAllState 并返回 String 的方法。
 */
@FunctionalInterface
public interface EdgeAction {
    String apply(OverAllState state) throws Exception;
}
