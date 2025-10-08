package com.coding.admin.repository;

import com.coding.admin.model.model.WorkflowInstanceModel;

/**
 * 工作流实例Repository接口
 * @author coding
 * @date 2025-09-21
 */
public interface WorkflowInstanceRepository {

    /**
     * 新增
     */
    Long add(WorkflowInstanceModel workflowInstanceModel);

    /**
     * 更新
     */
    int update(WorkflowInstanceModel workflowInstanceModel);

    /**
     * 删除
     */
    int delete(String id);

    /**
     * 根据ID查询
     */
    WorkflowInstanceModel getById(String id);
}
