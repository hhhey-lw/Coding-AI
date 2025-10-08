package com.coding.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例表DO
 * @author coding
 * @date 2025-09-21
 */
@Data
@TableName("workflow_instance")
public class WorkflowInstanceDO {

    /**
     * 工作流执行实例ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作流配置Id
     */
    @TableField("workflow_config_id")
    private Long workflowConfigId;

    /**
     * 所属应用ID
     */
    @TableField("app_id")
    private Long appId;

    /**
     * 使用的版本号
     */
    @TableField("version")
    private String version;

    /**
     * 工作流输入参数
     */
    @TableField("input_params")
    private String inputParams;

    /**
     * 执行状态：RUNNING / SUCCESS / FAILED / PAUSED / STOPPED
     */
    @TableField("status")
    private String status;

    /**
     * 执行人
     */
    @TableField("creator")
    private Long creator;

    /**
     * 工作流开始执行时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 工作流结束执行时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

}
