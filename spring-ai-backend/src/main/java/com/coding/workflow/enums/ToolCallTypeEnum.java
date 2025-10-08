package com.coding.workflow.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 枚举，表示系统中不同类型的工具调用。每种类型对应客户端与工具之间的特定交互模式。
 *
 * @since 1.0.0.3
 */
@Getter
@AllArgsConstructor
public enum ToolCallTypeEnum {

	/** 标准函数调用类型 */
	@JsonProperty("function")
	FUNCTION("function"),

	/** 基础工具调用类型 */
	@JsonProperty("tool_call")
	TOOL_CALL("tool_call"),

	/** 工具调用的结果 */
	@JsonProperty("tool_result")
	TOOL_RESULT("tool_result"),

	/** MCP 工具调用类型 */
	@JsonProperty("mcp_tool_call")
	MCP_TOOL_CALL("mcp_tool_call"),

	/** MCP 工具调用的结果 */
	@JsonProperty("mcp_tool_result")
	MCP_TOOL_RESULT("mcp_tool_result"),

	/** 组件专用工具调用类型 */
	@JsonProperty("component_tool_call")
	COMPONENT_TOOL_CALL("component_tool_call"),

	/** 组件工具调用的结果 */
	@JsonProperty("component_tool_result")
	COMPONENT_TOOL_RESULT("component_tool_result"),

	/** 文件检索操作调用类型 */
	@JsonProperty("file_search_call")
	FILE_SEARCH_CALL("file_search_call"),

	/** 文件检索操作的结果 */
	@JsonProperty("file_search_result")
	FILE_SEARCH_RESULT("file_search_result"),;

	/** 表示该工具调用类型的字符串值 */
	private final String value;

}