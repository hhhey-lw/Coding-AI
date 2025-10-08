package com.coding.admin.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.coding.admin.mapper.WorkflowNodeInstanceMapper;
import com.coding.admin.model.converter.WorkflowNodeInstanceConverter;
import com.coding.admin.model.entity.WorkflowNodeInstanceDO;
import com.coding.admin.model.model.WorkflowNodeInstanceModel;
import com.coding.admin.repository.WorkflowNodeInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点执行实例Repository实现类
 * @author coding
 * @date 2025-09-21
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WorkflowNodeInstanceRepositoryImpl implements WorkflowNodeInstanceRepository {

    private final WorkflowNodeInstanceMapper workflowNodeInstanceMapper;

    @Override
    public int add(WorkflowNodeInstanceModel workflowNodeInstanceModel) {
        WorkflowNodeInstanceDO entity = WorkflowNodeInstanceConverter.INSTANCE.modelToDO(workflowNodeInstanceModel);
        return workflowNodeInstanceMapper.insert(entity);
    }

    @Override
    public int update(WorkflowNodeInstanceModel workflowNodeInstanceModel) {
        return workflowNodeInstanceMapper.updateById(WorkflowNodeInstanceConverter.INSTANCE.modelToDO(workflowNodeInstanceModel));
    }

    @Override
    public int updateByNodeId(WorkflowNodeInstanceModel workflowNodeInstanceModel) {
        WorkflowNodeInstanceDO nodeDO = WorkflowNodeInstanceConverter.INSTANCE.modelToDO(workflowNodeInstanceModel);
        LambdaUpdateWrapper<WorkflowNodeInstanceDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WorkflowNodeInstanceDO::getNodeId, nodeDO.getNodeId())
                     .eq(WorkflowNodeInstanceDO::getWorkflowInstanceId, nodeDO.getWorkflowInstanceId());
        return workflowNodeInstanceMapper.update(nodeDO, updateWrapper);
    }

    @Override
    public int delete(String id) {
        return workflowNodeInstanceMapper.deleteById(id);
    }

    @Override
    public WorkflowNodeInstanceModel getById(String id) {
        return WorkflowNodeInstanceConverter.INSTANCE.doToModel(workflowNodeInstanceMapper.selectById(id));
    }

    @Override
    public List<WorkflowNodeInstanceModel> getByWorkflowInstanceId(Long workflowInstanceId) {
        LambdaQueryWrapper<WorkflowNodeInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkflowNodeInstanceDO::getWorkflowInstanceId, workflowInstanceId);
        List<WorkflowNodeInstanceDO> nodeInstanceDOList = workflowNodeInstanceMapper.selectList(queryWrapper);
        return nodeInstanceDOList.stream()
                .map(WorkflowNodeInstanceConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());
    }
}
