package com.coding.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.model.model.WorkflowConfigModel;
import com.coding.core.model.response.WorkflowRunningResult;

import java.util.List;
import java.util.Map;

/**
 * 工作流配置Service接口
 * @author coding
 * @date 2025-09-21
 */
public interface WorkflowConfigService {

    /**
     * 新增工作流配置
     * @param workflowConfigModel 工作流配置模型
     * @return 新增的工作流配置ID
     */
    Long addWorkflowConfig(WorkflowConfigModel workflowConfigModel);

    /**
     * 创建和修改工作流配置
     */
    boolean updateWorkflowConfig(WorkflowConfigModel workflowConfigModel);

    /**
     * 根据ID获取工作流配置
     */
    WorkflowConfigModel getWorkflowConfigById(String id);

    /**
     * 运行工作流
     * @param workflowId 工作流ID
     * @param inputParams 输入参数
     * @return 执行实例ID
     */
    Long runWorkflow(String workflowId, Map<String, Object> inputParams);

    /**
     * 查询工作流执行结果
     */
    WorkflowRunningResult getWorkflowRunningResult(String workflowInstanceId);

    /**
     * 查询当前用户的工作流列表
     * @return 工作流配置列表
     */
    List<WorkflowConfigModel> getMyWorkflowList();

    /**
     * 分页查询当前用户的工作流列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param workflowName 工作流名称（模糊查询）
     * @return 分页结果
     */
    Page<WorkflowConfigModel> getMyWorkflowPage(String workflowName, Integer pageNum, Integer pageSize);
}
