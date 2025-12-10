package com.coding.agentflow.service.tool;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具管理器
 * 统一管理本地工具和 MCP 工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolManager {

    private final Map<String, AgentTool> toolMap;

    /**
     * 获取指定名称的工具执行器
     */
    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 执行指定工具
     */
    public Object executeTool(String name, Map<String, Object> params) {
        AgentTool tool = getTool(name);
        if (tool == null) {
            throw new IllegalArgumentException("工具不存在: " + name);
        }
        return tool.execute(params);
    }

    /**
     * 获取工具回调列表（用于 LLM）
     * @param toolNames 允许使用的Tool列表
     */
    public List<ToolCallback> getToolCallbacks(Set<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }
        return toolNames.stream()
                .map(this::getTool)
                .filter(Objects::nonNull)
                .filter(agentTool -> toolNames.contains(agentTool.getName()))
                .map(AgentTool::getToolCallback)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有支持的工具列表
     */
    public List<ToolInfo> getAllTools() {
        return toolMap.values().stream()
                .map(tool -> new ToolInfo(tool.getName(), tool.getDescription(), tool.getParameters()))
                .collect(Collectors.toList());
    }

    public record ToolInfo(String name, String description, Map<String, String> params) {}
}
