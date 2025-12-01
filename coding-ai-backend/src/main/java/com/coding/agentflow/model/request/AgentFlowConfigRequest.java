package com.coding.agentflow.model.request;

import com.coding.agentflow.model.model.Edge;
import com.coding.agentflow.model.model.Node;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Agent流程配置请求对象
 */
@Data
@Schema(description = "Agent流程配置请求")
public class AgentFlowConfigRequest {

    @Schema(description = "配置ID（更新时必填）")
    private Long id;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "节点列表")
    private List<Node> nodes;

    @Schema(description = "边列表")
    private List<Edge> edges;

    @Schema(description = "状态 0:禁用 1:启用")
    private Integer status;
}
