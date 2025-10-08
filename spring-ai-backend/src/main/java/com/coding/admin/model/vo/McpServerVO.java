package com.coding.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "Mcp可用服务VO")
public class McpServerVO {

    @Schema(name = "服务名称")
    private String serverName;

    /** 服务的唯一标识 */
    @JsonProperty("server_code")
    @Schema(name = "服务标识")
    private String serverCode;

    @JsonProperty("tool_name")
    @Schema(name = "工具名称")
    private String toolName;

    @JsonProperty("tool_params")
    @Schema(name = "工具参数", description = "keyName, keyType")
    private List<ToolParam> toolParams;

    @JsonProperty("return_description")
    @Schema(name = "返回内容描述")
    private String contentDescription;

    @Data
    public static class ToolParam {
        private String key;
        private String type;
        private String desc;

        public static ToolParam of(String key, String type, String desc) {
            ToolParam toolParam = new ToolParam();
            toolParam.setKey(key);
            toolParam.setType(type);
            toolParam.setDesc(desc);
            return toolParam;
        }
    }
}
