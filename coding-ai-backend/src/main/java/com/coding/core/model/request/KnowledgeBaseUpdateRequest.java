package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新知识库请求
 * @author weilong
 */
@Data
@Schema(description = "更新知识库请求")
public class KnowledgeBaseUpdateRequest {

    @NotNull(message = "知识库ID不能为空")
    @Schema(description = "知识库ID", required = true)
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}

