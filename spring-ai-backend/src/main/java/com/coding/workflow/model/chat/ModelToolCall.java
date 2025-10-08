package com.coding.workflow.model.chat;

import com.coding.workflow.enums.ToolCallTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表示聊天系统中的工具调用，包含被调用工具及其执行详情的信息。
 *
 * @since 1.0.0.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelToolCall implements Serializable {

	/** 工具调用的唯一标识符 */
	private String id;

	/** 工具调用的类型 */
	private ToolCallTypeEnum type;

	/** 工具调用在序列中的索引 */
	private Integer index;

	/** 工具调用的函数详情 */
	private Function function;

	/**
	 * 表示工具调用的函数详情。
	 */
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Function implements Serializable {

		/** 函数名称 */
		private String name;

		/** 传递给函数的参数 */
		private String arguments;

		/** 函数执行的输出结果 */
		private String output;

	}

}