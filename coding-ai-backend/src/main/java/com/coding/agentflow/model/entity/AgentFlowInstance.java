package com.coding.agentflow.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AgentFlow 运行实例
 */
@Data
@TableName("agent_flow_instance")
@Builder
public class AgentFlowInstance {

    /**
     * 运行实例ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的 AgentConfig ID
     */
    @TableField("agent_config_id")
    private Long agentConfigId;

    /**
     * 运行状态 (例如: RUNNING, SUCCESS, FAILED)
     */
    @TableField("status")
    private String status;

    /**
     * 初始输入参数 (JSON字符串)
     */
    @TableField("input_data")
    private String inputData;

    /**
     * 最终输出结果 (JSON字符串)
     */
    @TableField("output_data")
    private String outputData;

    /**
     * 错误信息 (如果失败)
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
