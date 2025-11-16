package com.coding.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流实例VO
 * @author coding
 * @date 2025-09-21
 */
@Data
@Schema(description = "工作流实例VO")
public class WorkflowInstanceVO {

    @Schema(description = "工作流执行实例ID")
    private Long id;

    @Schema(description = "工作流配置Id")
    private Long workflowConfigId;

    @Schema(description = "所属应用ID")
    private Long appId;

    @Schema(description = "使用的版本号")
    private String version;

    @Schema(description = "工作流输入参数")
    private Map<String, Object> inputParams;

    @Schema(description = "执行状态：RUNNING / SUCCESS / FAILED / PAUSED / STOPPED")
    private String status;

    @Schema(description = "工作流开始执行时间")
    private LocalDateTime startTime;

    @Schema(description = "工作流结束执行时间")
    private LocalDateTime endTime;

    @Schema(description = "执行人")
    private Long creator;

}
