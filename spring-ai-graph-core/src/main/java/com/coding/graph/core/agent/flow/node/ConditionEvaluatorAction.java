package com.coding.graph.core.agent.flow.node;


import com.coding.graph.core.node.action.AsyncEdgeAction;
import com.coding.graph.core.state.OverAllState;

import java.util.concurrent.CompletableFuture;

/**
 * 异步边动作，用于评估条件以确定路由路径。
 * 此类从状态中读取条件结果并返回相应的路由。
 */
public class ConditionEvaluatorAction implements AsyncEdgeAction {

	private static final String CONDITION_KEY = "_condition_result";

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();

		try {
			// 从状态中获取条件结果
			String conditionResult = state.value(CONDITION_KEY, "default");
			result.complete(conditionResult);
		}
		catch (Exception e) {
			result.completeExceptionally(e);
		}

		return result;
	}

}
