package com.coding.core.repository;

import com.coding.core.model.model.ChatMessageModel;

import java.util.Collection;
import java.util.List;

/**
 * 聊天消息Repository接口
 * @author coding
 * @date 2025-10-28
 */
public interface ChatMessageRepository {

    /**
     * 新增消息
     */
    Long add(ChatMessageModel chatMessageModel);

    /**
     * 批量插入
     */
    Integer add(Collection<ChatMessageModel> chatMessageModels);

    /**
     * 更新消息
     */
    int update(ChatMessageModel chatMessageModel);

    /**
     * 删除消息
     */
    int delete(Long id);

    /**
     * 根据ID查询
     */
    ChatMessageModel getById(Long id);

    /**
     * 根据会话ID查询消息列表
     */
    List<ChatMessageModel> getByConversationId(String conversationId);

    /**
     * 根据会话ID删除消息
     */
    int deleteByConversationId(String conversationId);
}

