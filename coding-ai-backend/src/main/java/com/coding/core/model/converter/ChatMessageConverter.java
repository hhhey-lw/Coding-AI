package com.coding.core.model.converter;

import com.coding.core.model.entity.ChatMessageDO;
import com.coding.core.model.model.ChatMessageModel;
import com.coding.core.model.vo.ChatMessageVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 聊天消息转换器
 * @author coding
 * @date 2025-10-28
 */
@Mapper
public interface ChatMessageConverter {

    ChatMessageConverter INSTANCE = Mappers.getMapper(ChatMessageConverter.class);

    /**
     * String转List<Message>
     */
    default List<Message> mapStringToListMessage(String messagesJson) {
        if (messagesJson == null || messagesJson.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(messagesJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse messages JSON: " + messagesJson, e);
        }
    }

    /**
     * List<Message>转String
     */
    default String mapListMessageToString(List<Message> messages) {
        if (messages == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize messages to JSON: " + messages, e);
        }
    }

    /**
     * DO转Model
     */
    ChatMessageModel doToModel(ChatMessageDO chatMessageDO);

    /**
     * Model转VO
     */
    ChatMessageVO modelToVO(ChatMessageModel chatMessageModel);

    /**
     * Model转DO
     */
    ChatMessageDO modelToDO(ChatMessageModel chatMessageModel);
}

