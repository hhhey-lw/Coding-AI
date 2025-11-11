package com.coding.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新知识向量请求
 * @author weilong
 */
@Data
@Schema(description = "更新知识向量请求")
public class KnowledgeVectorUpdateRequest {

    @NotBlank(message = "向量ID不能为空")
    @Schema(description = "向量ID", required = true)
    private String id;

    @Schema(description = "文档内容")
    private String content;

    @Schema(description = "元数据（JSON格式）")
    private String metadata;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;
}

