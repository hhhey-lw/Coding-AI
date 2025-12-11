package com.coding.agentflow.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory 配置类
 * 用于 LLM 节点和 Agent 节点的对话记忆功能
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 创建 ChatMemory Bean
     * 使用 MessageWindowChatMemory，保留最近的消息窗口
     * 后续可以替换为 Redis 或数据库实现
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)  // 默认保留最近20条消息
                .build();
    }
}
