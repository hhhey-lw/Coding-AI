package com.coding.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天消息实体
 * @author coding
 * @date 2025-10-28
 */
@Data
@TableName("chat_message")
public class ChatMessageDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 消息内容（JSON格式）
     */
    @TableField("messages")
    private String messages;

    /**
     * 消息类型
     */
    @TableField("type")
    private String type;
}

