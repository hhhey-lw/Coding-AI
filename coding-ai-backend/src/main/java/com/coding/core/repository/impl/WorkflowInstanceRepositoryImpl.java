package com.coding.core.repository.impl;

import com.coding.core.mapper.WorkflowInstanceMapper;
import com.coding.core.model.converter.WorkflowInstanceConverter;
import com.coding.core.model.entity.WorkflowInstanceDO;
import com.coding.core.model.model.WorkflowInstanceModel;
import com.coding.core.repository.WorkflowInstanceRepository;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
