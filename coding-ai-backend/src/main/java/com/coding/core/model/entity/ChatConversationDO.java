package com.coding.core.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话实体
 * @author coding
 * @date 2025-10-28
 */
@Data
@TableName("chat_conversation")
public class ChatConversationDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId("id")
    private String id;

    /**
     * 发起会话的用户ID，可为空如果为匿名
     */
    @TableField("user_id")
    private String userId;

    /**
     * 会话标题，例如用户自定义或系统生成
     */
    @TableField("title")
    private String title;

    /**
     * 会话创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 会话状态：active, archived, deleted
     */
    @TableField("status")
    private String status;
}

