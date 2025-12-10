package com.coding.agentflow.service.tool.impl;

import com.coding.agentflow.service.tool.AbstractAgentTool;
import com.coding.workflow.manager.McpServerManager;
import com.coding.workflow.model.chat.TextContent;
import com.coding.workflow.model.request.McpServerCallToolRequest;
import com.coding.workflow.model.response.McpServerCallToolResponse;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component("webSearch")
public class WebSearchTool extends AbstractAgentTool<WebSearchTool.Request, WebSearchTool.Response> {

    /**
     * 默认搜索结果数量
     */
    public static final int DEFAULT_SEARCH_COUNT = 10;

    @Resource
    private McpServerManager mcpServerManager;

    @Override
    public String getName() {
        return "webSearch";
    }

    @Override
    public String getDescription() {
        return "联网搜索实时信息";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
                "query", "查询内容",
                "count", "返回的搜索结果数量"
        );
    }

    @Override
    protected Class<Request> getInputType() {
        return Request.class;
    }

    @Override
    public Response apply(Request request) {
        McpServerCallToolRequest mcpRequest = new McpServerCallToolRequest();
        mcpRequest.setRequestId(UUID.randomUUID().toString());
        mcpRequest.setServerCode("bocha_search");
        mcpRequest.setToolName("bocha_web_search");
        mcpRequest.setToolParams(Map.of(
                "query", request.query,
                "count", DEFAULT_SEARCH_COUNT
        ));

        McpServerCallToolResponse mcpServerCallToolResponse = mcpServerManager.callTool(mcpRequest);
        return new Response(mcpServerCallToolResponse.getContent().stream()
                .map(content -> {
                    if (content instanceof TextContent) {
                        return ((TextContent) content).getText();
                    }
                    return "";
                })
                .collect(Collectors.joining("\n")));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("联网搜索请求")
    public record Request(
            @JsonProperty(required = true, value = "query")
            @JsonPropertyDescription("搜索关键词") String query
    ) {}

    public record Response(String results) {}

}
