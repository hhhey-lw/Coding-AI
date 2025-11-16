package com.coding.core.service;

import com.coding.core.model.entity.AiMcpConfigDO;
import com.coding.core.model.vo.AiMcpConfigVO;

import java.util.List;

/**
 * MCP服务器服务接口
 */
public interface AiMcpConfigService {

    /**
     * 获取所有启用的MCP服务器列表
     */
    List<AiMcpConfigVO> getEnabledMcpServers();

    /**
     * 根据服务代码获取MCP服务器配置
     */
    AiMcpConfigDO getByServerCode(String serverCode);
}
