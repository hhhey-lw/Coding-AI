package com.coding.agentflow.model.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(name = "工作流连接边")
public class Edge {

    @Schema(name = "边唯一标识")
    private String id;

    @Schema(name = "标签")
    private String label;

    @Schema(name = "源节点ID")
    private String source;

    @Schema(name = "源节点句柄")
    private String sourceHandle;

    @Schema(name = "目标节点ID")
    private String target;

    @Schema(name = "目标节点句柄")
    private String targetHandle;

}