package com.coding.agentflow.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent配置实体类
 */
@Data
@TableName("agent_flow_config")
public class AgentFlowConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent名称
     */
    @TableField("name")
    private String name;

    /**
     * Agent描述
     */
    @TableField("description")
    private String description;

    /**
     * 节点配置 (JSON字符串)
     */
    @TableField("nodes")
    private String nodes;

    /**
     * 边配置 (JSON字符串)
     */
    @TableField("edges")
    private String edges;

    /**
     * 状态 (0:禁用 1:启用)
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 更新人ID
     */
    @TableField("updater_id")
    private Long updaterId;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

}
