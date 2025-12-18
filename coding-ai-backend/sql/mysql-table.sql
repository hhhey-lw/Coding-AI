/*
 Navicat Premium Dump SQL

 Source Server         : Coding-AI
 Source Server Type    : MySQL
 Source Server Version : 50740 (5.7.40-log)
 Source Host           : 121.40.252.207:3306
 Source Schema         : ai-service

 Target Server Type    : MySQL
 Target Server Version : 50740 (5.7.40-log)
 File Encoding         : 65001

 Date: 18/12/2025 11:15:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_flow_config
-- ----------------------------
DROP TABLE IF EXISTS `agent_flow_config`;
CREATE TABLE `agent_flow_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) DEFAULT NULL COMMENT '工作流名称',
  `description` text COMMENT '工作流描述',
  `nodes` longtext COMMENT '节点列表(JSON)',
  `edges` longtext COMMENT '边列表(JSON)',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `updater_id` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `creator_id` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COMMENT='Agent流程配置表';

-- ----------------------------
-- Table structure for agent_flow_instance
-- ----------------------------
DROP TABLE IF EXISTS `agent_flow_instance`;
CREATE TABLE `agent_flow_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '运行实例ID',
  `agent_config_id` bigint(20) NOT NULL COMMENT '关联的配置ID',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/PAUSED/SUCCESS/FAILED',
  `input_data` longtext COMMENT '初始输入参数(JSON)',
  `output_data` longtext COMMENT '最终输出结果(JSON)',
  `error_msg` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `creator_id` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`agent_config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2001482519624015874 DEFAULT CHARSET=utf8mb4 COMMENT='Agent流程运行实例表';

-- ----------------------------
-- Table structure for agent_flow_node_instance
-- ----------------------------
DROP TABLE IF EXISTS `agent_flow_node_instance`;
CREATE TABLE `agent_flow_node_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '节点实例ID',
  `agent_instance_id` bigint(20) NOT NULL COMMENT '关联的流程实例ID',
  `node_id` varchar(64) NOT NULL COMMENT '节点ID(配置中的nodeId)',
  `node_type` varchar(32) DEFAULT NULL COMMENT '节点类型',
  `node_name` varchar(255) DEFAULT NULL COMMENT '节点名称',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/PAUSED/SUCCESS/FAILED',
  `input_data` longtext COMMENT '节点输入数据(JSON)',
  `output_data` longtext COMMENT '节点输出结果(JSON)',
  `error_msg` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `creator_id` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_instance_id` (`agent_instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COMMENT='Agent流程节点运行实例表';

-- ----------------------------
-- Table structure for chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS `chat_conversation`;
CREATE TABLE `chat_conversation` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发起会话的用户ID，可为空如果为匿名',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题，例如用户自定义或系统生成',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '会话状态：active, archived, deleted',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id_create_time` (`user_id`,`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='会话管理表';

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `messages` text COLLATE utf8mb4_unicode_ci,
  `type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=942 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='聊天记录表';

-- ----------------------------
-- Table structure for resource_record
-- ----------------------------
DROP TABLE IF EXISTS `resource_record`;
CREATE TABLE `resource_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称(UUID)',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件访问URL',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小(字节)',
  `file_md5` varchar(32) NOT NULL COMMENT '文件MD5值',
  `content_type` varchar(100) NOT NULL COMMENT 'MIME类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_file_md5` (`file_md5`) USING BTREE COMMENT 'MD5唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='文件信息表';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_account` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `user_password` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `user_name` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `user_profile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `user_role` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1939706750165798919 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';

-- ----------------------------
-- Table structure for workflow_config
-- ----------------------------
DROP TABLE IF EXISTS `workflow_config`;
CREATE TABLE `workflow_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工作流定义唯一ID',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流名称',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作流描述',
  `app_id` bigint(20) NOT NULL COMMENT '所属应用/项目ID',
  `version` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1.0' COMMENT '版本号',
  `nodes` json NOT NULL COMMENT '节点列表，JSON 格式，存储所有节点定义',
  `edges` json NOT NULL COMMENT '边列表，JSON 格式，存储节点之间的连接关系',
  `canvas` text COLLATE utf8mb4_unicode_ci COMMENT '前端画布信息',
  `creator` bigint(20) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0-禁用，1-启用',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2000758164609818626 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义表';

-- ----------------------------
-- Table structure for workflow_instance
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance`;
CREATE TABLE `workflow_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '工作流执行实例ID',
  `workflow_config_id` bigint(20) NOT NULL COMMENT '工作流配置Id',
  `app_id` bigint(20) NOT NULL COMMENT '所属应用ID',
  `version` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用的版本号',
  `input_params` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作流实例运行参数',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行状态：RUNNING / SUCCESS / FAILED / PAUSED / STOPPED',
  `creator` bigint(20) DEFAULT NULL COMMENT '执行人',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1974402118002122871 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工作流实例表';

-- ----------------------------
-- Table structure for workflow_node_instance
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_instance`;
CREATE TABLE `workflow_node_instance` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `node_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对应 workflow_definition 中的节点ID，如：LLM、Start',
  `node_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点类型：start / llm / judge / end 等',
  `node_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称（可冗余）',
  `workflow_instance_id` bigint(20) NOT NULL COMMENT '关联的workflow_instance.id',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点执行状态：PENDING / RUNNING / SUCCESS / FAILED / SKIPPED',
  `input` json DEFAULT NULL COMMENT '节点执行时的输入参数',
  `output` json DEFAULT NULL COMMENT '节点执行后的输出结果',
  `start_time` datetime DEFAULT NULL COMMENT '节点开始执行时间',
  `execute_time` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点执行时间',
  `error_info` text COLLATE utf8mb4_unicode_ci COMMENT '节点执行错误信息',
  `error_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点错误码',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_instance_id` (`workflow_instance_id`) USING BTREE COMMENT '加速按工作流实例查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='节点执行实例表';

SET FOREIGN_KEY_CHECKS = 1;
