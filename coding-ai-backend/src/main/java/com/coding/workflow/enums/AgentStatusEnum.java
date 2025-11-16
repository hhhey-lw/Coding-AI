/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coding.workflow.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 表示系统中代理的状态。
 *
 */
@Getter
@AllArgsConstructor
public enum AgentStatusEnum {

	/** 表示代理已成功完成任务 */
	@JsonProperty("completed")
	COMPLETED("completed"),

	/** 表示代理任务失败 */
	@JsonProperty("failed")
	FAILED("failed"),

	/** 表示代理正在处理任务 */
	@JsonProperty("in_progress")
	IN_PROGRESS("in_progress"),

	/** 表示代理任务未完成 */
	@JsonProperty("incomplete")
	INCOMPLETE("incomplete"),;

	/** 表示状态的字符串值 */
	private final String value;

	/**
	 * 将完成原因字符串转换为对应的 AgentStatus。
	 * @param finishReason 任务完成原因
	 * @return 对应的 AgentStatus
	 */
	public static AgentStatusEnum toAgentStatus(String finishReason) {
		if (finishReason == null || finishReason.isEmpty()) {
			return AgentStatusEnum.IN_PROGRESS;
		}

		finishReason = finishReason.toLowerCase();
		return switch (finishReason) {
			case "stop", "length" -> AgentStatusEnum.COMPLETED;
			case "tool_calls" -> AgentStatusEnum.IN_PROGRESS;
			default -> AgentStatusEnum.FAILED;
		};
	}

}
