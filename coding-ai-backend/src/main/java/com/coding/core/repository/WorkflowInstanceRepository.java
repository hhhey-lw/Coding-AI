package com.coding.core.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.model.model.WorkflowInstanceModel;

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

    /**
     * 分页查询用户的工作流运行记录
     * @param userId 用户ID
     * @param workflowConfigId 工作流配置ID（可选）
     * @param status 执行状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<WorkflowInstanceModel> pageByUserId(Long userId, Long workflowConfigId, String status, Integer pageNum, Integer pageSize);
}
