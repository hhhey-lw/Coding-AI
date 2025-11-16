package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流配置状态更新请求")
public class WorkflowConfigUpdateStatusRequest {

    @Schema(description = "工作流定义唯一ID")
    private String id;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

}
