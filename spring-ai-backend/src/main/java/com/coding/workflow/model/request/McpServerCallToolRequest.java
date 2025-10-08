package com.coding.workflow.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * @author weilong
 * @date 2025/9/28
 */
@Data
public class McpServerCallToolRequest {

    /** 请求的唯一标识 */
    @JsonProperty("request_id")
    private String requestId;

    /** 服务的唯一标识 */
    @JsonProperty("server_code")
    private String serverCode;

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("tool_params")
    private Map<String, Object> toolParams;

}
