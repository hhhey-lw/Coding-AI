package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.WorkflowNodeInstanceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节点执行实例Mapper
 * @author coding
 * @date 2025-09-21
 */
@Mapper
public interface WorkflowNodeInstanceMapper extends BaseMapper<WorkflowNodeInstanceDO> {
}
