package com.coding.agentflow.service.tool;

import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * 统一工具接口
 * 屏蔽本地工具、MCP工具、API工具的差异
 */
public interface AgentTool {

    /**
     * 获取工具名称（唯一标识）
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 工具参数： key: 参数名, value: 参数描述
     */
    Map<String, String> getParameters();

    /**
     * 获取 Spring AI 所需的 ToolCallback
     * 用于大模型函数调用
     */
    ToolCallback getToolCallback();

    /**
     * 直接执行工具
     * 用于非大模型场景或测试
     *
     * @param args 工具参数
     * @return 执行结果
     */
    Object execute(Map<String, Object> args);
}
