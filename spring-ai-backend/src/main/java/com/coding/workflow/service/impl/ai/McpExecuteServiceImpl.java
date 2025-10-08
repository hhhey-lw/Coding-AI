package com.coding.workflow.service.impl.ai;

import com.coding.admin.common.Result;
import com.coding.workflow.service.ai.McpServerService;
import com.coding.workflow.manager.McpServerManager;
import com.coding.workflow.model.chat.Content;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;
import com.coding.workflow.model.chat.TextContent;
import io.modelcontextprotocol.spec.McpError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author weilong
 * @date 2025/9/28
 */
@Slf4j
@Service
public class McpExecuteServiceImpl implements McpServerService {

    @Resource
    private McpServerManager mcpServerManager;

    public Result<McpServerCallToolResponse> callTool(McpServerCallToolRequest request) {
        try {

            McpServerCallToolResponse response = mcpServerManager.callTool(request);

            return Result.success(response);
        } catch (Exception e) {
            log.error("调用MCP工具失败", e);
            if (e instanceof McpError) {
                return buildErrorResult(e);
            }
        }
        return Result.error("调用MCP工具失败");
    }

    /**
     * 构建错误结果
     */
    private static Result<McpServerCallToolResponse> buildErrorResult(Exception e) {
        McpServerCallToolResponse errorResponse = new McpServerCallToolResponse();
        errorResponse.setIsError(true);
        TextContent content = new TextContent();
        content.setText(e.getMessage());
        List<Content> contentList = new ArrayList<>();
        contentList.add(content);
        errorResponse.setContent(contentList);
        return Result.success(errorResponse);
    }

}
