package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增知识向量请求
 * @author weilong
 */
@Data
@Schema(description = "新增知识向量请求")
public class KnowledgeVectorAddRequest {

    @NotNull(message = "知识库ID不能为空")
    @Schema(description = "知识库ID", required = true)
    private Long knowledgeBaseId;

    @NotBlank(message = "文档内容不能为空")
    @Schema(description = "文档内容", required = true)
    private String content;

    @Schema(description = "元数据（JSON格式）")
    private String metadata;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;
}

