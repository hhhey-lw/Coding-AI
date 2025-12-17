package com.coding.core.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识向量VO
 * @author weilong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识向量信息")
public class KnowledgeVectorVO {

    @Schema(description = "向量ID")
    private String id;

    @Schema(description = "所属知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "文档内容")
    private String content;

    @Schema(description = "元数据（JSON格式）")
    private String metadata;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

