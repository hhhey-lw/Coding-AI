package com.coding.graph.core.node.command;

import com.coding.graph.core.node.config.RunnableConfig;
import com.coding.graph.core.state.OverAllState;

@FunctionalInterface
public interface CommandAction {

	Command apply(OverAllState state, RunnableConfig config) throws Exception;

}