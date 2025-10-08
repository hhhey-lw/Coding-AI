package com.coding.admin.model.converter;

import com.coding.admin.model.entity.WorkflowInstanceDO;
import com.coding.admin.model.model.WorkflowInstanceModel;
import com.coding.admin.model.vo.WorkflowInstanceVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工作流实例转换器
 * @author coding
 * @date 2025-09-21
 */
@Mapper(uses = StringMapConverter.class)
public interface WorkflowInstanceConverter {

    WorkflowInstanceConverter INSTANCE = Mappers.getMapper(WorkflowInstanceConverter.class);

    /**
     * DO转Model
     */
    WorkflowInstanceModel doToModel(WorkflowInstanceDO workflowInstanceDO);

    /**
     * Model转VO
     */
    WorkflowInstanceVO modelToVO(WorkflowInstanceModel workflowInstanceModel);

    /**
     * Model转DO
     */
    WorkflowInstanceDO modelToDO(WorkflowInstanceModel workflowInstanceModel);
}
