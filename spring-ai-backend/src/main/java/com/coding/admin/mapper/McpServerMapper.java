package com.coding.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.admin.model.entity.McpServerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP服务器Mapper
 */
@Mapper
public interface McpServerMapper extends BaseMapper<McpServerDO> {

    /**
     * 查询所有启用的MCP服务器
     */
    List<McpServerDO> selectEnabledServers();
}
