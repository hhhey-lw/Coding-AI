package com.coding.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.model.converter.ChatConversationConverter;
import com.coding.admin.model.model.ChatConversationModel;
import com.coding.admin.model.vo.ChatConversationVO;
import com.coding.admin.repository.ChatConversationRepository;
import com.coding.admin.service.ChatConversationService;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话服务实现类
 * @author coding
 * @date 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl implements ChatConversationService {

    private final ChatConversationRepository chatConversationRepository;

    @Override
    public String createConversation(ChatConversationModel chatConversationModel) {
        return chatConversationRepository.add(chatConversationModel);
    }

    @Override
    public int updateConversation(ChatConversationModel chatConversationModel) {
        log.info("更新聊天会话，id: {}", chatConversationModel.getId());
        
        // 更新时间
        chatConversationModel.setUpdatedAt(LocalDateTime.now());
        
        return chatConversationRepository.update(chatConversationModel);
    }

    @Override
    public int deleteConversation(String id) {
        log.info("删除聊天会话，id: {}", id);
        
        // 软删除：更新状态为deleted
        ChatConversationModel model = chatConversationRepository.getById(id);
        if (model == null) {
            throw new BizException("会话不存在");
        }
        
        model.setStatus("deleted");
        model.setUpdatedAt(LocalDateTime.now());
        
        return chatConversationRepository.update(model);
    }

    @Override
    public ChatConversationVO getById(String id) {
        ChatConversationModel model = chatConversationRepository.getById(id);
        return ChatConversationConverter.INSTANCE.modelToVO(model);
    }

    @Override
    public List<ChatConversationVO> getByUserId(String userId) {
        List<ChatConversationModel> modelList = chatConversationRepository.getByUserId(userId);
        return modelList.stream()
                .map(ChatConversationConverter.INSTANCE::modelToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ChatConversationVO> pageByUserId(String userId, String status, Integer pageNum, Integer pageSize) {
        Page<ChatConversationModel> modelPage = chatConversationRepository.pageByUserId(userId, status, pageNum, pageSize);
        
        // 转换为VO
        List<ChatConversationVO> voList = modelPage.getRecords().stream()
                .map(ChatConversationConverter.INSTANCE::modelToVO)
                .collect(Collectors.toList());
        
        // 构建分页结果
        Page<ChatConversationVO> voPage = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        voPage.setRecords(voList);
        voPage.setTotal(modelPage.getTotal());
        
        return voPage;
    }

    @Override
    public List<ChatConversationVO> getByUserIdAndStatus(String userId, String status) {
        List<ChatConversationModel> modelList = chatConversationRepository.getByUserIdAndStatus(userId, status);
        return modelList.stream()
                .map(ChatConversationConverter.INSTANCE::modelToVO)
                .collect(Collectors.toList());
    }
}

