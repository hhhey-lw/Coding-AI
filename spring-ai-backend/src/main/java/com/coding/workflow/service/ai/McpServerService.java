package com.coding.workflow.service.ai;

import com.coding.admin.common.Result;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;

/**
 * @author weilong
 * @date 2025/9/28
 */
public interface McpServerService {

    Result<McpServerCallToolResponse> callTool(McpServerCallToolRequest request);

}
