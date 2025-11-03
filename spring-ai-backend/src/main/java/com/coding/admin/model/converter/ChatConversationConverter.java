package com.coding.admin.model.converter;

import com.coding.admin.model.entity.ChatConversationDO;
import com.coding.admin.model.model.ChatConversationModel;
import com.coding.admin.model.vo.ChatConversationVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 聊天会话转换器
 * @author coding
 * @date 2025-10-28
 */
@Mapper
public interface ChatConversationConverter {

    ChatConversationConverter INSTANCE = Mappers.getMapper(ChatConversationConverter.class);

    /**
     * DO转Model
     */
    ChatConversationModel doToModel(ChatConversationDO chatConversationDO);

    /**
     * Model转VO
     */
    ChatConversationVO modelToVO(ChatConversationModel chatConversationModel);

    /**
     * Model转DO
     */
    ChatConversationDO modelToDO(ChatConversationModel chatConversationModel);
}

