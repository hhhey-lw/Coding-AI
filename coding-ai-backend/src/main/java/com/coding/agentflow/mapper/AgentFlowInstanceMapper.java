package com.coding.agentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.agentflow.model.entity.AgentFlowInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * AgentFlow 运行实例 Mapper 接口
 */
@Mapper
public interface AgentFlowInstanceMapper extends BaseMapper<AgentFlowInstance> {
}
