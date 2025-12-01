package com.coding.agentflow.service.node;

import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具节点
 * 调用外部工具或API执行特定功能
 */
@Slf4j
@Component
public class ToolNode extends AbstractNode {

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String toolName = getConfigParamAsString(node, "toolName", "");
        Object toolParamsObj = getConfigParam(node, "toolParams");
        @SuppressWarnings("unchecked")
        Map<String, Object> toolParams = toolParamsObj instanceof Map ? (Map<String, Object>) toolParamsObj : new HashMap<>();

        log.info("执行工具节点，工具名称: {}, 参数: {}", toolName, JSONUtil.toJsonStr(toolParams));

        // TODO: 根据工具类型调用相应的工具
        Object toolResult = invokeTool(toolName, toolParams, context);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("toolName", toolName);
        resultData.put("toolParams", toolParams);
        resultData.put("toolResult", toolResult);

        return NodeExecutionResult.success(resultData);
    }

    /**
     * 调用工具
     */
    private Object invokeTool(String toolName, Map<String, Object> params, Map<String, Object> context) {
        // TODO: 实现工具调用逻辑
        // 支持多种工具类型：API调用、数据库查询、文件操作等
        return Map.of(
                "status", "success",
                "message", "工具调用结果（待实现）",
                "data", new HashMap<>()
        );
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String toolName = getConfigParamAsString(node, "toolName", null);
        if (toolName == null || toolName.isEmpty()) {
            log.error("工具节点缺少必需的toolName配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.TOOL.name();
    }
}
