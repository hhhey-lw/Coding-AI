package com.coding.agentflow.repository;

import com.coding.agentflow.model.entity.AgentFlowInstance;
import com.coding.agentflow.model.entity.AgentFlowNodeInstance;

/**
 * AgentFlow 运行实例 Repository 接口
 */
public interface AgentFlowInstanceRepository {

    /**
     * 保存工作流实例
     */
    void saveFlowInstance(AgentFlowInstance instance);

    /**
     * 更新工作流实例
     */
    void updateFlowInstance(AgentFlowInstance instance);

    /**
     * 根据ID获取工作流实例
     */
    AgentFlowInstance getFlowInstanceById(Long id);

    /**
     * 保存节点实例
     */
    void saveNodeInstance(AgentFlowNodeInstance nodeInstance);

    /**
     * 更新节点实例
     */
    void updateNodeInstance(AgentFlowNodeInstance nodeInstance);

    /**
     * 根据工作流实例ID和节点ID获取节点实例
     */
    AgentFlowNodeInstance getNodeInstance(Long agentInstanceId, String nodeId);
}
