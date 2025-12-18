package com.coding.core.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 聊天消息服务接口
 * @author coding
 * @date 2025-10-28
 */
public interface ChatMessageService {


    /**
     * 获取消息列表
     *
     * @param conversationId
     * @return
     */
    List<Message> findMessages(String conversationId);


    /**
     * 保存用户消息，追加模式
     * @param conversationId
     * @param messages
     */
    void saveMessages(String conversationId, List<Message> messages);

    /**
     * 移除未完成的工具调用请求
     * @param conversationId
     */
    void removeIncompleteToolCalls(String conversationId);
}

