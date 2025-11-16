package com.coding.core.model.request;

import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowConfigAddRequest {

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流描述")
    private String description;

    @Schema(description = "所属应用/项目ID")
    @JsonProperty("app_id")
    private Long appId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "节点列表，JSON 格式，存储所有节点定义")
    private List<Node> nodes;

    @Schema(description = "边列表，JSON 格式，存储节点之间的连接关系")
    private List<Edge> edges;

    @Schema(description = "画布信息")
    private String canvas;

}
