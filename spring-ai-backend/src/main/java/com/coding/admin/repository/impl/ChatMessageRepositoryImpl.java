package com.coding.admin.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.admin.mapper.ChatMessageMapper;
import com.coding.admin.model.converter.ChatMessageConverter;
import com.coding.admin.model.entity.ChatMessageDO;
import com.coding.admin.model.model.ChatMessageModel;
import com.coding.admin.repository.ChatMessageRepository;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天消息Repository实现类
 * @author coding
 * @date 2025-10-28
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Long add(ChatMessageModel chatMessageModel) {
        ChatMessageDO chatMessageDO = ChatMessageConverter.INSTANCE.modelToDO(chatMessageModel);
        int insert = chatMessageMapper.insert(chatMessageDO);
        if (insert == 0) {
            throw new BizException("新增聊天消息失败");
        }
        return chatMessageDO.getId();
    }

    @Override
    public Integer add(Collection<ChatMessageModel> chatMessageModels) {
        if (chatMessageModels == null || chatMessageModels.isEmpty()) {
            return 0;
        }
        
        List<ChatMessageDO> dos = chatMessageModels.stream()
                .map(ChatMessageConverter.INSTANCE::modelToDO)
                .toList();
        
        // 使用循环逐个插入（简单可靠）
        int count = 0;
        for (ChatMessageDO chatMessageDO : dos) {
            int insert = chatMessageMapper.insert(chatMessageDO);
            count += insert;
        }
        
        return count;
    }

    @Override
    public int update(ChatMessageModel chatMessageModel) {
        return chatMessageMapper.updateById(ChatMessageConverter.INSTANCE.modelToDO(chatMessageModel));
    }

    @Override
    public int delete(Long id) {
        return chatMessageMapper.deleteById(id);
    }

    @Override
    public ChatMessageModel getById(Long id) {
        return ChatMessageConverter.INSTANCE.doToModel(chatMessageMapper.selectById(id));
    }

    @Override
    public List<ChatMessageModel> getByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getConversationId, conversationId)
               .orderByAsc(ChatMessageDO::getId);

        List<ChatMessageDO> chatMessageDOList = chatMessageMapper.selectList(wrapper);

        return chatMessageDOList.stream()
                .map(ChatMessageConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getConversationId, conversationId);
        return chatMessageMapper.delete(wrapper);
    }
}

