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

@Component("mapAroundSearch")
public class MapAroundSearchTool extends AbstractAgentTool<MapAroundSearchTool.Request, MapAroundSearchTool.Response> {

    @Resource
    private McpServerManager mcpServerManager;

    @Override
    public String getName() {
        return "mapAroundSearch";
    }

    @Override
    public String getDescription() {
        return "地图周边搜索：根据中心点、半径和关键词搜索附近的地点（POI）信息";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
                "location", "搜索中心点(经纬度坐标)",
                "keywords", "搜索关键字",
                "radius", "搜索半径(米)"
        );
    }

    @Override
    protected Class<MapAroundSearchTool.Request> getInputType() {
        return Request.class;
    }

    @Override
    public Response apply(Request request) {
        McpServerCallToolRequest mcpRequest = new McpServerCallToolRequest();
        mcpRequest.setRequestId(UUID.randomUUID().toString());
        mcpRequest.setServerCode("gaode_map");
        mcpRequest.setToolName("maps_around_search");
        mcpRequest.setToolParams(Map.of(
                "location", request.location,
                "keywords", request.keyword,
                "radius", request.radius
        ));

        McpServerCallToolResponse toolResponse = mcpServerManager.callTool(mcpRequest);
        return new Response(toolResponse.getContent().stream().map(content -> {
            if (content instanceof TextContent) {
                return ((TextContent) content).getText();
            }
            return "";
        }).collect(Collectors.joining("\n")));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("地图周边搜索请求")
    public record Request(
            @JsonProperty(required = true, value = "location")
            @JsonPropertyDescription("搜索中心点（经纬度坐标，如116.397,39.908）") String location,

            @JsonProperty(required = true, value = "keyword")
            @JsonPropertyDescription("搜索关键词（如'咖啡厅'、'停车场'）") String keyword,

            @JsonProperty(required = true, value = "radius")
            @JsonPropertyDescription("搜索半径（单位：米）") Integer radius
    ) {}

    public record Response(String results) {}

}
