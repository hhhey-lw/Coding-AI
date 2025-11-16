package com.coding.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.utils.UserContextHolder;
import com.coding.core.model.converter.WorkflowInstanceConverter;
import com.coding.core.model.converter.WorkflowNodeInstanceConverter;
import com.coding.core.model.model.WorkflowConfigModel;
import com.coding.core.model.model.WorkflowInstanceModel;
import com.coding.core.model.model.WorkflowNodeInstanceModel;
import com.coding.core.model.response.WorkflowRunningResult;
import com.coding.core.model.vo.WorkflowInstanceVO;
import com.coding.core.model.vo.WorkflowNodeInstanceVO;
import com.coding.core.repository.WorkflowConfigRepository;
import com.coding.core.repository.WorkflowInstanceRepository;
import com.coding.core.repository.WorkflowNodeInstanceRepository;
import com.coding.core.service.WorkflowConfigService;
import com.coding.workflow.enums.WorkflowStatusEnum;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.service.WorkflowCoreEngine;
import com.coding.workflow.utils.AssertUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流配置Service实现类
 * @author coding
 * @date 2025-09-21
 */
@Service
public class WorkflowConfigServiceImpl implements WorkflowConfigService {

    @Resource
    private WorkflowCoreEngine workflowCoreEngine;

    @Resource
    private Map<String, AbstractExecuteProcessor> executeProcessorMap;

    @Resource
    private WorkflowConfigRepository workflowConfigRepository;

    @Resource
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Resource
    private WorkflowNodeInstanceRepository workflowNodeInstanceRepository;

    @Override
    public Long addWorkflowConfig(WorkflowConfigModel workflowConfigModel) {
        workflowConfigModel.setCreator(UserContextHolder.getUserId());
        return workflowConfigRepository.add(workflowConfigModel);
    }

    @Override
    public boolean updateWorkflowConfig(WorkflowConfigModel workflowConfigModel) {
        return workflowConfigRepository.update(workflowConfigModel) > 0;
    }

    @Override
    public WorkflowConfigModel getWorkflowConfigById(String id) {
        return workflowConfigRepository.getById(id);
    }

    @Override
    public Long runWorkflow(String workflowId, Map<String, Object> inputParams) {

        WorkflowConfigModel workflowConfigModel = workflowConfigRepository.getById(workflowId);
        AssertUtil.isNotNull(workflowConfigModel, "工作流配置不存在，ID：" + workflowId);

        return workflowCoreEngine.executeWorkflow(workflowConfigModel, inputParams);
    }

    @Override
    public WorkflowRunningResult getWorkflowRunningResult(String workflowInstanceId) {
        // 根据实例ID查询工作流实例
        WorkflowInstanceModel workflowInstanceModel = workflowInstanceRepository.getById(workflowInstanceId);
        AssertUtil.isNotNull(workflowInstanceModel, "工作流实例不存在，ID：" + workflowInstanceId);
        
        // 根据工作流实例ID查询所有节点实例
        List<WorkflowNodeInstanceModel> nodeInstanceModels = workflowNodeInstanceRepository.getByWorkflowInstanceId(workflowInstanceModel.getId());
        
        // 转换为VO对象
        WorkflowInstanceVO workflowInstanceVO = WorkflowInstanceConverter.INSTANCE.modelToVO(workflowInstanceModel);
        List<WorkflowNodeInstanceVO> nodeInstanceVOList = nodeInstanceModels.stream()
                .map(WorkflowNodeInstanceConverter.INSTANCE::modelToVO)
                .collect(Collectors.toList());
        
        // 构建结果对象
        WorkflowRunningResult result = new WorkflowRunningResult();
        result.setWorkflowInstanceVO(workflowInstanceVO);
        result.setWorkflowNodeInstanceVOList(nodeInstanceVOList);
        
        return result;
    }

    @Override
    public List<WorkflowConfigModel> getMyWorkflowList() {
        // 从UserContextHolder获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();
        AssertUtil.isNotNull(userId, "用户未登录");

        // 查询当前用户创建的所有工作流
        return workflowConfigRepository.getByUserId(userId);
    }

    @Override
    public Page<WorkflowConfigModel> getMyWorkflowPage(String workflowName, Integer pageNum, Integer pageSize) {
        // 从UserContextHolder获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();
        AssertUtil.isNotNull(userId, "用户未登录");

        // 调用Repository分页查询
        return workflowConfigRepository.pageByUserId(userId, workflowName, pageNum, pageSize);
    }

    @Override
    public Page<WorkflowInstanceModel> getMyWorkflowInstancePage(Long workflowConfigId, String status, Integer pageNum, Integer pageSize) {
        // 从UserContextHolder获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();
        AssertUtil.isNotNull(userId, "用户未登录");

        // 调用Repository分页查询工作流运行记录
        return workflowInstanceRepository.pageByUserId(userId, workflowConfigId, status, pageNum, pageSize);
    }

    // ==========> 辅助方法 <==========

    @PostConstruct
    private void init() {
        // 注册所有的执行器
        for (Map.Entry<String, AbstractExecuteProcessor> processorEntry : executeProcessorMap.entrySet()) {
            workflowCoreEngine.registerProcessor(processorEntry.getValue().getNodeType(), processorEntry.getValue());
        }
    }

    /** 构建工作流执行实例模型 */
    private WorkflowInstanceModel buildWorkflowInstanceModel(WorkflowConfigModel workflowConfigModel, Map<String, Object> inputParams) {
        return WorkflowInstanceModel.builder()
                .appId(workflowConfigModel.getAppId())
                .version(workflowConfigModel.getVersion())
                .workflowConfigId(workflowConfigModel.getId())
                .startTime(LocalDateTime.now())
                .creator(-1L)
                .inputParams(inputParams)
                .status(WorkflowStatusEnum.EXECUTING.getCode())
                .build();
    }

}
