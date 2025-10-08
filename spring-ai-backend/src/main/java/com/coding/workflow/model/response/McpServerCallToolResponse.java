package com.coding.workflow.model.response;

import com.coding.workflow.model.chat.Content;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author weilong
 * @date 2025/9/28
 */
@Data
public class McpServerCallToolResponse {
    @JsonProperty("is_error")
    private Boolean isError;

    /** 工具调用返回的内容 */
    private List<Content> content;

}
