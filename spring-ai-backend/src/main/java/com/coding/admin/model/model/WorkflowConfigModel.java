package com.coding.admin.model.model;

import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流配置Model
 * @author coding
 * @date 2025-09-21
 */
@Data
@Schema(description = "工作流配置Model")
public class WorkflowConfigModel {

    @Schema(description = "工作流定义唯一ID")
    private Long id;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流描述")
    private String description;

    @Schema(description = "所属应用/项目ID")
    private Long appId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "节点列表，JSON 格式，存储所有节点定义")
    private List<Node> nodes;

    @Schema(description = "边列表，JSON 格式，存储节点之间的连接关系")
    private List<Edge> edges;

    @Schema(description = "画布信息")
    private String canvas;

    @Schema(description = "创建人")
    private Long creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
