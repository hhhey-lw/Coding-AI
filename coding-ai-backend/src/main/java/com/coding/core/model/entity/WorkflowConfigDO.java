package com.coding.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流定义表DO
 * @author coding
 * @date 2025-09-21
 */
@Data
@TableName("workflow_config")
public class WorkflowConfigDO {

    /**
     * 工作流定义唯一ID
     */
    @TableId
    private Long id;

    /**
     * 工作流名称
     */
    @TableField("name")
    private String name;

    /**
     * 工作流描述
     */
    @TableField("description")
    private String description;

    /**
     * 所属应用/项目ID
     */
    @TableField("app_id")
    private Long appId;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 节点列表，JSON 格式，存储所有节点定义
     */
    @TableField("nodes")
    private String nodes;

    /**
     * 边列表，JSON 格式，存储节点之间的连接关系
     */
    @TableField("edges")
    private String edges;

    /**
     * 画布信息
     */
    @TableField("canvas")
    private String canvas;

    /**
     * 创建人
     */
    @TableField("creator")
    private Long creator;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;
}
