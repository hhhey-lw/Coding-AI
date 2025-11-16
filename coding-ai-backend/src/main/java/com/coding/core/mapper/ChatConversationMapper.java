package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.ChatConversationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话Mapper
 * @author coding
 * @date 2025-10-28
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversationDO> {
}

