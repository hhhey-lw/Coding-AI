package com.coding.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识向量分页查询请求
 * @author weilong
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识向量分页查询请求")
public class KnowledgeVectorPageRequest extends PageRequest {

    @NotNull(message = "知识库ID不能为空")
    @Schema(description = "知识库ID", required = true)
    private Long knowledgeBaseId;

    @Schema(description = "文件名（模糊查询）")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;
}

