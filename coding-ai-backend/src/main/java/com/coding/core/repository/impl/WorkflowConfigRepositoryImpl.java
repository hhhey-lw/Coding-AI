package com.coding.core.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.mapper.WorkflowConfigMapper;
import com.coding.core.model.converter.WorkflowConfigConverter;
import com.coding.core.model.entity.WorkflowConfigDO;
import com.coding.core.model.model.WorkflowConfigModel;
import com.coding.core.repository.WorkflowConfigRepository;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流配置Repository实现类
 * @author coding
 * @date 2025-09-21
 */
@Repository
@RequiredArgsConstructor
public class WorkflowConfigRepositoryImpl implements WorkflowConfigRepository {

    private final WorkflowConfigMapper workflowConfigMapper;

    @Override
    public Long add(WorkflowConfigModel workflowConfigModel) {
        WorkflowConfigDO workflowConfigDO = WorkflowConfigConverter.INSTANCE.modelToDO(workflowConfigModel);
        int insert = workflowConfigMapper.insert(workflowConfigDO);
        if (insert == 0) {
            throw new BizException("新增工作流配置失败");
        }
        return workflowConfigDO.getId();
    }

    @Override
    public int update(WorkflowConfigModel workflowConfigModel) {
        return workflowConfigMapper.updateById(WorkflowConfigConverter.INSTANCE.modelToDO(workflowConfigModel));
    }

    @Override
    public int delete(String id) {
        return workflowConfigMapper.deleteById(id);
    }

    @Override
    public WorkflowConfigModel getById(String id) {
        return WorkflowConfigConverter.INSTANCE.doToModel(workflowConfigMapper.selectById(id));
    }

    @Override
    public List<WorkflowConfigModel> getByUserId(Long creatorId) {
        LambdaQueryWrapper<WorkflowConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowConfigDO::getCreator, creatorId)
               .orderByDesc(WorkflowConfigDO::getCreateTime);

        List<WorkflowConfigDO> workflowConfigDOList = workflowConfigMapper.selectList(wrapper);

        return workflowConfigDOList.stream()
                .map(WorkflowConfigConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());
    }

    @Override
    public Page<WorkflowConfigModel> pageByUserId(Long userId, String workflowName, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<WorkflowConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowConfigDO::getCreator, userId)
               .like(StringUtils.isNotBlank(workflowName), WorkflowConfigDO::getName, workflowName)
               .eq(WorkflowConfigDO::getStatus, Boolean.TRUE)
               .orderByDesc(WorkflowConfigDO::getCreateTime);

        // 使用MyBatis-Plus的分页查询
        Page<WorkflowConfigDO> doPage = new Page<>(pageNum, pageSize);
        workflowConfigMapper.selectPage(doPage, wrapper);

        // 转换为Model对象
        List<WorkflowConfigModel> modelList = doPage.getRecords().stream()
                .map(WorkflowConfigConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());

        // 构建分页结果
        Page<WorkflowConfigModel> modelPage = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        modelPage.setRecords(modelList);
        modelPage.setTotal(doPage.getTotal());

        return modelPage;
    }
}
