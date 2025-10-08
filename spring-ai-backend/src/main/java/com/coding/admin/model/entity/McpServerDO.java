package com.coding.admin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP服务器实体
 */
@Data
@TableName("mcp_server")
public class McpServerDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("provider_code")
    private String providerCode;

    @TableField("server_name")
    private String serverName;

    @TableField("server_code")
    private String serverCode;

    @TableField("endpoint")
    private String endpoint;

    @TableField("tool_name")
    private String toolName;

    @TableField("tool_params")
    private String toolParams;

    @TableField("content_description")
    private String contentDescription;

    @TableField("call_count")
    private Long callCount;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
