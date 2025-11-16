package com.coding.core.model.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 节点执行实例Model
 * @author coding
 * @date 2025-09-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点执行实例Model")
public class WorkflowNodeInstanceModel {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "对应 workflow_definition 中的节点ID")
    private String nodeId;

    @Schema(description = "节点类型：start / llm / judge / end 等")
    private String nodeType;

    @Schema(description = "节点名称（可冗余）")
    private String nodeName;

    @Schema(description = "关联的workflow_instance.id")
    private Long workflowInstanceId;

    @Schema(description = "节点执行状态：PENDING / RUNNING / SUCCESS / FAILED / SKIPPED")
    private String status;

    @Schema(description = "节点执行时的输入参数")
    private String input;

    @Schema(description = "节点执行后的输出结果")
    private String output;

    @Schema(description = "节点开始执行时间")
    private LocalDateTime startTime;

    @Schema(description = "节点结束执行时间")
    private String executeTime;

    @Schema(description = "节点执行错误信息")
    private String errorInfo;

    @Schema(description = "节点错误码")
    private String errorCode;

}
