package com.coding.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.model.model.ChatConversationModel;
import com.coding.admin.model.vo.ChatConversationVO;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 聊天会话服务接口
 * @author coding
 * @date 2025-10-28
 */
public interface ChatConversationService {

    /**
     * 创建会话
     */
    String createConversation(ChatConversationModel chatConversationModel);

    /**
     * 更新会话
     */
    int updateConversation(ChatConversationModel chatConversationModel);

    /**
     * 删除会话
     */
    int deleteConversation(String id);

    /**
     * 根据ID查询会话
     */
    ChatConversationVO getById(String id);

    /**
     * 根据用户ID查询会话列表
     */
    List<ChatConversationVO> getByUserId(String userId);

    /**
     * 根据用户ID分页查询会话列表
     */
    Page<ChatConversationVO> pageByUserId(String userId, String status, Integer pageNum, Integer pageSize);

    /**
     * 根据用户ID和状态查询会话列表
     */
    List<ChatConversationVO> getByUserIdAndStatus(String userId, String status);

}

