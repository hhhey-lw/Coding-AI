package com.coding.admin.service;

import com.coding.admin.model.entity.McpServerDO;
import com.coding.admin.model.vo.McpServerVO;

import java.util.List;

/**
 * MCP服务器服务接口
 */
public interface McpServerService {

    /**
     * 获取所有启用的MCP服务器列表
     */
    List<McpServerVO> getEnabledMcpServers();

    /**
     * 根据服务代码获取MCP服务器配置
     */
    McpServerDO getByServerCode(String serverCode);
}
