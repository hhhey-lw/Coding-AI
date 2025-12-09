package com.coding.agentflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coding.agentflow.model.entity.AgentFlowConfig;
import com.coding.agentflow.model.request.AgentFlowConfigRequest;
import com.coding.agentflow.model.response.AgentFlowConfigResponse;

/**
 * Agent流程配置 服务类
 */
public interface AgentFlowConfigService extends IService<AgentFlowConfig> {

    /**
     * 分页查询 Agent 流程配置
     *
     * @param current 当前页
     * @param size    每页大小
     * @param name    名称（模糊查询）
     * @return 分页结果
     */
    Page<AgentFlowConfigResponse> pageAgentFlows(Integer current, Integer size, String name);

    /**
     * 根据ID查询配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    AgentFlowConfigResponse getAgentFlowById(Long id);

    /**
     * 保存或更新配置
     *
     * @param request 请求对象
     * @return id
     */
    Long saveOrUpdateAgentFlow(AgentFlowConfigRequest request);

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    Boolean removeAgentFlow(Long id);
}
