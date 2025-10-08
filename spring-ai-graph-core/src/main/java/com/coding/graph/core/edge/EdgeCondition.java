package com.coding.graph.core.edge;

import com.coding.graph.core.node.command.AsyncCommandAction;

import java.util.Map;

/**
 * 边条件，包含异步命令行为和映射关系
 *
 * @param action   异步命令行为
 * @param mappings 映射关系列表：key:终点，value:标签(条件)
 */
public record EdgeCondition(AsyncCommandAction action, Map<String, String> mappings) {
}
