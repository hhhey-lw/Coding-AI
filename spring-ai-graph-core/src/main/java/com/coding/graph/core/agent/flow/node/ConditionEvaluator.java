package com.coding.graph.core.agent.flow.node;


import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.state.OverAllState;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件评估节点动作，用于评估条件以确定下一个执行路径。
 * 此类检查状态并设置条件标志以进行路由决策。
 */
public class ConditionEvaluator implements NodeAction {

	private static final String CONDITION_KEY = "_condition_result";

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> updatedState = new HashMap<>();

		// 默认条件评估逻辑
		// 可以扩展以支持自定义条件评估
		String conditionResult = evaluateCondition(state);
		updatedState.put(CONDITION_KEY, conditionResult);

		return updatedState;
	}

	/**
	 * 根据当前状态评估条件。重写此方法以实现自定义条件逻辑。
	 * @param state 当前状态
	 * @return 条件结果字符串
	 */
	protected String evaluateCondition(OverAllState state) {
		// 简单示例：检查输入是否包含特定关键字
		String input = state.value("input", "").toString().toLowerCase();

		if (input.contains("error") || input.contains("exception")) {
			return "error_handling";
		}
		else if (input.contains("data") || input.contains("analyze")) {
			return "data_processing";
		}
		else if (input.contains("report") || input.contains("summary")) {
			return "report_generation";
		}
		else {
			return "default";
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		public ConditionEvaluator build() {
			return new ConditionEvaluator();
		}

	}

}
