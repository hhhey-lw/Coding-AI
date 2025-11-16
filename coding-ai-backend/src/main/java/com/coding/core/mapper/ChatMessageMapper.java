package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.ChatMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 * @author coding
 * @date 2025-10-28
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {
}

