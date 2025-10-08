package com.coding.admin.model.converter;

import com.coding.admin.model.entity.WorkflowNodeInstanceDO;
import com.coding.admin.model.model.WorkflowNodeInstanceModel;
import com.coding.admin.model.vo.WorkflowNodeInstanceVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 节点执行实例转换器
 * @author coding
 * @date 2025-09-21
 */
@Mapper
public interface WorkflowNodeInstanceConverter {

    WorkflowNodeInstanceConverter INSTANCE = Mappers.getMapper(WorkflowNodeInstanceConverter.class);

    /**
     * DO转Model
     */
    WorkflowNodeInstanceModel doToModel(WorkflowNodeInstanceDO workflowNodeInstanceDO);

    /**
     * Model转VO
     */
    WorkflowNodeInstanceVO modelToVO(WorkflowNodeInstanceModel workflowNodeInstanceModel);

    /**
     * Model转DO
     */
    WorkflowNodeInstanceDO modelToDO(WorkflowNodeInstanceModel workflowNodeInstanceModel);
}
