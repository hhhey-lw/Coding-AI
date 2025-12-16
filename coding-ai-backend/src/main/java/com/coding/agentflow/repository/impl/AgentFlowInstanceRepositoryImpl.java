package com.coding.agentflow.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.agentflow.mapper.AgentFlowInstanceMapper;
import com.coding.agentflow.mapper.AgentFlowNodeInstanceMapper;
import com.coding.agentflow.model.entity.AgentFlowInstance;
import com.coding.agentflow.model.entity.AgentFlowNodeInstance;
import com.coding.agentflow.repository.AgentFlowInstanceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * AgentFlow 运行实例 Repository 实现类
 */
@Repository
@AllArgsConstructor
public class AgentFlowInstanceRepositoryImpl implements AgentFlowInstanceRepository {

    private final AgentFlowInstanceMapper agentFlowInstanceMapper;
    private final AgentFlowNodeInstanceMapper agentFlowNodeInstanceMapper;

    @Override
    public void saveFlowInstance(AgentFlowInstance instance) {
        agentFlowInstanceMapper.insert(instance);
    }

    @Override
    public void updateFlowInstance(AgentFlowInstance instance) {
        agentFlowInstanceMapper.updateById(instance);
    }

    @Override
    public AgentFlowInstance getFlowInstanceById(Long id) {
        return agentFlowInstanceMapper.selectById(id);
    }

    @Override
    public void saveNodeInstance(AgentFlowNodeInstance nodeInstance) {
        agentFlowNodeInstanceMapper.insert(nodeInstance);
    }

    @Override
    public void updateNodeInstance(AgentFlowNodeInstance nodeInstance) {
        agentFlowNodeInstanceMapper.updateById(nodeInstance);
    }

    @Override
    public AgentFlowNodeInstance getNodeInstance(Long agentInstanceId, String nodeId) {
        LambdaQueryWrapper<AgentFlowNodeInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentFlowNodeInstance::getAgentInstanceId, agentInstanceId)
                .eq(AgentFlowNodeInstance::getNodeId, nodeId);
        return agentFlowNodeInstanceMapper.selectOne(wrapper);
    }
}
