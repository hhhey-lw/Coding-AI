package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点执行实例VO
 * @author coding
 * @date 2025-09-21
 */
@Data
@Schema(description = "节点执行实例VO")
public class WorkflowNodeInstanceVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "对应 workflow_definition 中的节点ID")
    private String nodeId;

    @Schema(description = "节点类型：start / llm / judge / end 等")
    private String nodeType;

    @Schema(description = "节点名称（可冗余）")
    private String nodeName;

    @Schema(description = "关联的workflow_instance.id")
    private String workflowInstanceId;

    @Schema(description = "节点执行状态：PENDING / RUNNING / SUCCESS / FAILED / SKIPPED")
    private String status;

    @Schema(description = "节点执行时的输入参数")
    private String input;

    @Schema(description = "节点执行后的输出结果")
    private String output;

    @Schema(description = "节点开始执行时间")
    private LocalDateTime startTime;

    @Schema(description = "节点执行时间")
    private String executeTime;

    @Schema(description = "节点执行错误信息")
    private String errorInfo;

    @Schema(description = "节点错误码")
    private String errorCode;

}
