package com.coding.workflow.service.ai;

import com.coding.core.common.Result;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;

/**
 * @author weilong
 * @date 2025/9/28
 */
public interface McpExecuteService {

    /**
     * 调用MCP工具
     * */
    Result<McpServerCallToolResponse> callTool(McpServerCallToolRequest request);

}
