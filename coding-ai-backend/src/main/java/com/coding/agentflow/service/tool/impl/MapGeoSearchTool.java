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

@Component("mapGeoSearchTool")
public class MapGeoSearchTool extends AbstractAgentTool<MapGeoSearchTool.Request, MapGeoSearchTool.Response> {

    @Resource
    private McpServerManager mcpServerManager;

    @Override
    public String getName() {
        return "mapGeoSearchTool";
    }

    @Override
    public String getDescription() {
        return "将详细的结构化地址转换为经纬度坐标。支持对地标性名胜景区、建筑物名称解析为经纬度坐标";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
                "address", "待解析的结构化地址信息",
                "city", "查询指定城市"
        );
    }

    @Override
    protected Class<MapGeoSearchTool.Request> getInputType() {
        return MapGeoSearchTool.Request.class;
    }

    @Override
    public Response apply(Request request) {
        McpServerCallToolRequest mcpRequest = new McpServerCallToolRequest();
        mcpRequest.setRequestId(UUID.randomUUID().toString());
        mcpRequest.setServerCode("gaode_map");
        mcpRequest.setToolName("maps_geo");
        mcpRequest.setToolParams(Map.of(
                "address", request.address,
                "city", request.city
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
    @JsonClassDescription("地址转换为经纬度坐标")
    public record Request(
            @JsonProperty(required = true, value = "address")
            @JsonPropertyDescription("待解析的结构化地址信息") String address,

            @JsonProperty(required = false, value = "city")
            @JsonPropertyDescription("查询指定城市") String city
    ) {}

    public record Response(String results) {}

}
