package com.coding.agentflow.model.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作流整体结构
 */
@Data
@Schema(description = "工作流定义")
public class WorkflowConfig {

    @Schema(description = "工作流Id")
    private String id;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流描述")
    private String description;

    @Schema(description = "节点列表")
    private List<Node> nodes;

    @Schema(description = "边列表")
    private List<Edge> edges;

}
