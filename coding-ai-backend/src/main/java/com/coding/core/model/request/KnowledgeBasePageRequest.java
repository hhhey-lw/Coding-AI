package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询请求
 * @author weilong
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库分页查询请求")
public class KnowledgeBasePageRequest extends PageRequest {

    @Schema(description = "知识库名称（模糊查询）")
    private String name;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "创建用户ID")
    private Long userId;
}

