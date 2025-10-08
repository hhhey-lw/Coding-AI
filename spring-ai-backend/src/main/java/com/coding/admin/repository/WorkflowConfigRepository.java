package com.coding.admin.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.model.model.WorkflowConfigModel;

import java.util.List;

/**
 * 工作流配置Repository接口
 * @author coding
 * @date 2025-09-21
 */
public interface WorkflowConfigRepository {

    /**
     * 新增
     */
    Long add(WorkflowConfigModel workflowConfigModel);

    /**
     * 更新
     */
    int update(WorkflowConfigModel workflowConfigModel);

    /**
     * 删除
     */
    int delete(String id);

    /**
     * 根据ID查询
     */
    WorkflowConfigModel getById(String id);

    /**
     * 根据创建人ID查询工作流列表
     */
    List<WorkflowConfigModel> getByUserId(Long creatorId);

    /**
     * 分页查询用户的工作流列表
     */
    Page<WorkflowConfigModel> pageByUserId(Long userId, String workflowName, Integer pageNum, Integer pageSize);
}
