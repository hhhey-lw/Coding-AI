package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.WorkflowInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流实例Mapper
 * @author coding
 * @date 2025-09-21
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceDO> {
}
