package com.coding.agentflow.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.agentflow.mapper.AgentFlowConfigMapper;
import com.coding.agentflow.model.entity.AgentFlowConfig;
import com.coding.agentflow.repository.AgentFlowConfigRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

/**
 * Agent流程配置 Repository 实现类
 */
@Repository
@AllArgsConstructor
public class AgentFlowConfigRepositoryImpl implements AgentFlowConfigRepository {

    private final AgentFlowConfigMapper agentFlowConfigMapper;

    @Override
    public Page<AgentFlowConfig> pageAgentFlows(Integer current, Integer size, String name) {
        LambdaQueryWrapper<AgentFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), AgentFlowConfig::getName, name)
                .orderByDesc(AgentFlowConfig::getCreateTime);
        return agentFlowConfigMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
