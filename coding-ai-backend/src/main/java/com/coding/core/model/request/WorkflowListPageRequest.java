package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询我的工作流列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "查询我的工作流列表请求")
public class WorkflowListPageRequest extends PageRequest {

    @Schema(description = "工作流名称（模糊查询）")
    private String workflowName;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status;
}
