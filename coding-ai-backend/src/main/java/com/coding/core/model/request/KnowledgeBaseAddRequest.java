package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增知识库请求
 * @author weilong
 */
@Data
@Schema(description = "新增知识库请求")
public class KnowledgeBaseAddRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Schema(description = "知识库名称", required = true)
    private String name;

    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;
}

