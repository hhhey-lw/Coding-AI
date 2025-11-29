package com.coding.agentflow.model.workflow;

import com.coding.agentflow.model.workflow.enums.EdgeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(name = "工作流连接边")
public class Edge {

    @Schema(name = "边唯一标识")
    private String id;

    // 标记这是否是条件分支边
    @Schema(name = "边类型")
    private EdgeTypeEnum type;

    // 条件边的标识
    @Schema(name = "边标签/条件")
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