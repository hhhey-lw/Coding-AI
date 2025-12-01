package com.coding.agentflow.model.model;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流节点基类
 */
@Data
@Schema(name = "工作流节点基类")
public class Node {

    @Schema(name = "节点唯一标识")
    private String id;

    @Schema(name = "节点显示名称")
    private String label;

    @Schema(name = "节点类型")
    private NodeTypeEnum type;

    @Schema(name = "节点位置坐标")
    private Position position;

    @Schema(name = "节点静态配置参数（用户输入的配置）")
    private Map<String, Object> configParams;

    @Schema(name = "节点运行时输入参数（来自其他节点的数据）")
    private List<CommonParam> inputParams;

    @Schema(name = "节点运行时输出参数定义")
    private List<CommonParam> outputParams;

}