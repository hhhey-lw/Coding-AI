package com.coding.core.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.mapper.WorkflowInstanceMapper;
import com.coding.core.model.converter.WorkflowInstanceConverter;
import com.coding.core.model.entity.WorkflowInstanceDO;
import com.coding.core.model.model.WorkflowInstanceModel;
import com.coding.core.repository.WorkflowInstanceRepository;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 工作流实例Repository实现类
 * @author coding
 * @date 2025-09-21
 */
@Repository
@RequiredArgsConstructor
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    private final WorkflowInstanceMapper workflowInstanceMapper;

    @Override
    public Long add(WorkflowInstanceModel workflowInstanceModel) {
        WorkflowInstanceDO workflowInstanceDO = WorkflowInstanceConverter.INSTANCE.modelToDO(workflowInstanceModel);
        int insert = workflowInstanceMapper.insert(workflowInstanceDO);
        if (insert == 0) {
            throw new BizException("新增工作流实例失败");
        }
        return workflowInstanceDO.getId();
    }

    @Override
    public int update(WorkflowInstanceModel workflowInstanceModel) {
        return workflowInstanceMapper.updateById(WorkflowInstanceConverter.INSTANCE.modelToDO(workflowInstanceModel));
    }

    @Override
    public int delete(String id) {
        return workflowInstanceMapper.deleteById(id);
    }

    @Override
    public WorkflowInstanceModel getById(String id) {
        return WorkflowInstanceConverter.INSTANCE.doToModel(workflowInstanceMapper.selectById(id));
    }

    @Override
    public Page<WorkflowInstanceModel> pageByUserId(Long userId, Long workflowConfigId, String status, Integer pageNum, Integer pageSize) {
        // 构建查询条件
        LambdaQueryWrapper<WorkflowInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkflowInstanceDO::getCreator, userId);
        
        // 可选条件：工作流配置ID
        if (workflowConfigId != null) {
            queryWrapper.eq(WorkflowInstanceDO::getWorkflowConfigId, workflowConfigId);
        }
        
        // 可选条件：执行状态
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(WorkflowInstanceDO::getStatus, status);
        }
        
        // 按开始时间倒序排列
        queryWrapper.orderByDesc(WorkflowInstanceDO::getStartTime);
        
        // 分页查询
        Page<WorkflowInstanceDO> page = new Page<>(pageNum, pageSize);
        Page<WorkflowInstanceDO> resultPage = workflowInstanceMapper.selectPage(page, queryWrapper);
        
        // 转换为Model
        Page<WorkflowInstanceModel> modelPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        modelPage.setRecords(
            resultPage.getRecords().stream()
                .map(WorkflowInstanceConverter.INSTANCE::doToModel)
                .collect(java.util.stream.Collectors.toList())
        );
        
        return modelPage;
    }
}
