package com.coding.agentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.agentflow.model.entity.AgentFlowConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent流程配置 Mapper 接口
 */
@Mapper
public interface AgentFlowConfigMapper extends BaseMapper<AgentFlowConfig> {
}
