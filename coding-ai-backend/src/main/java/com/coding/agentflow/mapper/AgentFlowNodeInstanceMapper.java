package com.coding.agentflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.agentflow.model.entity.AgentFlowNodeInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * AgentFlow 节点运行实例 Mapper 接口
 */
@Mapper
public interface AgentFlowNodeInstanceMapper extends BaseMapper<AgentFlowNodeInstance> {
}
