package com.coding.agentflow.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.agentflow.model.entity.AgentFlowConfig;

/**
 * Agent流程配置 Repository 接口
 */
public interface AgentFlowConfigRepository {

    /**
     * 分页查询 Agent 流程配置
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  用户Id
     * @param name    名称（模糊查询）
     * @return 分页结果
     */
    Page<AgentFlowConfig> pageAgentFlows(Integer current, Integer size, Long userId, String name);
}
