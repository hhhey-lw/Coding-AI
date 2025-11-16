package com.coding.graph.core.node.factory;

import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.node.action.AsyncNodeActionWithConfig;
import com.coding.graph.core.node.config.CompileConfig;

public interface ActionFactory {

	AsyncNodeActionWithConfig apply(CompileConfig config) throws GraphStateException;

}