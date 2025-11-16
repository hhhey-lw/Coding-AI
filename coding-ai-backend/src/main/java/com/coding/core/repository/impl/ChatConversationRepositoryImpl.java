package com.coding.core.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.enums.DefaultStatusEnum;
import com.coding.core.mapper.ChatConversationMapper;
import com.coding.core.model.converter.ChatConversationConverter;
import com.coding.core.model.entity.ChatConversationDO;
import com.coding.core.model.model.ChatConversationModel;
import com.coding.core.repository.ChatConversationRepository;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话Repository实现类
 * @author coding
 * @date 2025-10-28
 */
@Repository
@RequiredArgsConstructor
public class ChatConversationRepositoryImpl implements ChatConversationRepository {

    private final ChatConversationMapper chatConversationMapper;

    @Override
    public String add(ChatConversationModel chatConversationModel) {
        ChatConversationDO chatConversationDO = ChatConversationConverter.INSTANCE.modelToDO(chatConversationModel);
        chatConversationDO.setStatus(DefaultStatusEnum.ACTIVE.getEnCode());
        chatConversationDO.setCreatedAt(LocalDateTime.now());
        chatConversationDO.setUpdatedAt(LocalDateTime.now());
        int insert = chatConversationMapper.insert(chatConversationDO);
        if (insert == 0) {
            throw new BizException("新增聊天会话失败");
        }
        return chatConversationDO.getId();
    }

    @Override
    public int update(ChatConversationModel chatConversationModel) {
        return chatConversationMapper.updateById(ChatConversationConverter.INSTANCE.modelToDO(chatConversationModel));
    }

    @Override
    public int delete(String id) {
        return chatConversationMapper.deleteById(id);
    }

    @Override
    public ChatConversationModel getById(String id) {
        return ChatConversationConverter.INSTANCE.doToModel(chatConversationMapper.selectById(id));
    }

    @Override
    public List<ChatConversationModel> getByUserId(String userId) {
        LambdaQueryWrapper<ChatConversationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationDO::getUserId, userId)
               .orderByDesc(ChatConversationDO::getUpdatedAt);

        List<ChatConversationDO> chatConversationDOList = chatConversationMapper.selectList(wrapper);

        return chatConversationDOList.stream()
                .map(ChatConversationConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ChatConversationModel> pageByUserId(String userId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ChatConversationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationDO::getUserId, userId)
               .eq(ChatConversationDO::getStatus, DefaultStatusEnum.ACTIVE.getEnCode())
               .orderByDesc(ChatConversationDO::getUpdatedAt);

        // 使用MyBatis-Plus的分页查询
        Page<ChatConversationDO> doPage = new Page<>(pageNum, pageSize);
        chatConversationMapper.selectPage(doPage, wrapper);

        // 转换为Model对象
        List<ChatConversationModel> modelList = doPage.getRecords().stream()
                .map(ChatConversationConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());

        // 构建分页结果
        Page<ChatConversationModel> modelPage = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        modelPage.setRecords(modelList);
        modelPage.setTotal(doPage.getTotal());

        return modelPage;
    }

    @Override
    public List<ChatConversationModel> getByUserIdAndStatus(String userId, String status) {
        LambdaQueryWrapper<ChatConversationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationDO::getUserId, userId)
               .eq(ChatConversationDO::getStatus, status)
               .orderByDesc(ChatConversationDO::getUpdatedAt);

        List<ChatConversationDO> chatConversationDOList = chatConversationMapper.selectList(wrapper);

        return chatConversationDOList.stream()
                .map(ChatConversationConverter.INSTANCE::doToModel)
                .collect(Collectors.toList());
    }
}

