package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowConfigUpdateRequest extends WorkflowConfigAddRequest {

    @Schema(description = "工作流定义唯一ID")
    private Long id;

}
