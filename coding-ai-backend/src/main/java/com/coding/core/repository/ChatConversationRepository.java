package com.coding.core.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.model.model.ChatConversationModel;

import java.util.List;

/**
 * 聊天会话Repository接口
 * @author coding
 * @date 2025-10-28
 */
public interface ChatConversationRepository {

    /**
     * 新增会话
     */
    String add(ChatConversationModel chatConversationModel);

    /**
     * 更新会话
     */
    int update(ChatConversationModel chatConversationModel);

    /**
     * 删除会话
     */
    int delete(String id);

    /**
     * 根据ID查询
     */
    ChatConversationModel getById(String id);

    /**
     * 根据用户ID查询会话列表
     */
    List<ChatConversationModel> getByUserId(String userId);

    /**
     * 根据用户ID分页查询会话列表
     */
    Page<ChatConversationModel> pageByUserId(String userId, String status, Integer pageNum, Integer pageSize);

    /**
     * 根据用户ID和状态查询会话列表
     */
    List<ChatConversationModel> getByUserIdAndStatus(String userId, String status);
}

