package com.coding.workflow.model.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流边模型
 */
@Data
public class Edge implements Serializable {
    /** 边Id */
    private String id;

    /** 起点节点Id */
    private String source;

    /** 如果起点节点存在多个出向连接点，⭐️前端展示使用。
     * sourceHandle则格式化为{source}_{连接点的id}；
     * 否则sourceHandle为{source}
     * */
    @JsonProperty("source_handle")
    private String sourceHandle;

    /** 终点节点Id */
    private String target;

    @JsonProperty("target_handle")
    private String targetHandle;
}
