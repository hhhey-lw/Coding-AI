package com.coding.admin.repository;

import com.coding.admin.model.model.WorkflowNodeInstanceModel;

/**
 * 节点执行实例Repository接口
 * @author coding
 * @date 2025-09-21
 */
public interface WorkflowNodeInstanceRepository {

    /**
     * 新增
     */
    int add(WorkflowNodeInstanceModel workflowNodeInstanceModel);

    /**
     * 更新
     */
    int update(WorkflowNodeInstanceModel workflowNodeInstanceModel);

    int updateByNodeId(WorkflowNodeInstanceModel workflowNodeInstanceModel);

    /**
     * 删除
     */
    int delete(String id);

    /**
     * 根据ID查询
     */
    WorkflowNodeInstanceModel getById(String id);

    /**
     * 根据工作流实例ID查询节点列表
     */
    java.util.List<WorkflowNodeInstanceModel> getByWorkflowInstanceId(Long workflowInstanceId);
}
