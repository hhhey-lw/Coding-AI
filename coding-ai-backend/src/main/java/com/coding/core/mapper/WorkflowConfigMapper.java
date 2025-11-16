package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.WorkflowConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流配置Mapper
 * @author coding
 * @date 2025-09-21
 */
@Mapper
public interface WorkflowConfigMapper extends BaseMapper<WorkflowConfigDO> {
}
