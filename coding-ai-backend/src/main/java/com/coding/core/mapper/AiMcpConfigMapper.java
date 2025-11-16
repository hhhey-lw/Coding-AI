package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.AiMcpConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @TODO 调整为字段表
 * MCP服务器Mapper
 */
@Mapper
public interface AiMcpConfigMapper extends BaseMapper<AiMcpConfigDO> {

    /**
     * 查询所有启用的MCP服务器
     */
    List<AiMcpConfigDO> selectEnabledServers();
}
