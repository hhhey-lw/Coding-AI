package com.coding.admin.model.model;

import com.coding.admin.model.entity.McpServerDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

@Data
@Schema(description = "MCP 服务配置信息")
public class McpServerModel {

    @Mapper
    public interface Convert {
        McpServerModel.Convert Instance = Mappers.getMapper(McpServerModel.Convert.class);

        McpServerModel toModel(McpServerDO entity);
    }

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "服务编码")
    private String serverCode;

    @Schema(description = "服务名称")
    private String name;

    @Schema(description = "服务描述")
    private String description;

    @Schema(description = "MCP服务主机地址，格式：https://xxx/mcp/")
    private String baseUrl;

    @Schema(description = "MCP服务端点路径，格式：xxx/sse")
    private String endpoint;

    @Schema(description = "服务器访问认证密钥（如 Bearer Token）")
    private String authorization;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}