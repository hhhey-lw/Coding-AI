package com.coding.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点执行实例表DO
 * @author coding
 * @date 2025-09-21
 */
@Data
@TableName("workflow_node_instance")
public class WorkflowNodeInstanceDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 对应 workflow_definition 中的节点ID，如：LLM、Start
     */
    @TableField("node_id")
    private String nodeId;

    /**
     * 节点类型：start / llm / judge / end 等
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 节点名称（可冗余）
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 关联的workflow_instance.id
     */
    @TableField("workflow_instance_id")
    private Long workflowInstanceId;

    /**
     * 节点执行状态：PENDING / RUNNING / SUCCESS / FAILED / SKIPPED
     */
    @TableField("status")
    private String status;

    /**
     * 节点执行时的输入参数
     */
    @TableField("input")
    private String input;

    /**
     * 节点执行后的输出结果
     */
    @TableField("output")
    private String output;

    /**
     * 节点开始执行时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 节点结束执行时间
     */
    @TableField("execute_time")
    private String executeTime;

    /**
     * 节点执行错误信息
     */
    @TableField("error_info")
    private String errorInfo;

    /**
     * 节点错误码
     */
    @TableField("error_code")
    private String errorCode;

}
