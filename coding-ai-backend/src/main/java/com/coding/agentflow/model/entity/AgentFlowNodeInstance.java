package com.coding.agentflow.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AgentFlow 节点运行实例
 */
@Data
@TableName("agent_flow_node_instance")
public class AgentFlowNodeInstance {

    /**
     * 节点实例ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的 AgentFlowInstance ID
     */
    @TableField("agent_instance_id")
    private Long agentInstanceId;

    /**
     * 节点ID (对应配置中的 nodeId)
     */
    @TableField("node_id")
    private String nodeId;

    /**
     * 节点类型 (例如: LLM, CONDITION, START, END)
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 运行状态 (PENDING, RUNNING, SUCCESS, FAILED)
     */
    @TableField("status")
    private String status;

    /**
     * 节点的输入数据 (JSON字符串)
     */
    @TableField("input_data")
    private String inputData;

    /**
     * 节点的输出结果 (JSON字符串)
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
