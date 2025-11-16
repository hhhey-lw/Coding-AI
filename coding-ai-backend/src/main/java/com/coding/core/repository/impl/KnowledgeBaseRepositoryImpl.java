package com.coding.core.repository.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.core.mapper.postgresql.KnowledgeBaseMapper;
import com.coding.core.model.entity.KnowledgeBaseDO;
import com.coding.core.model.request.KnowledgeBasePageRequest;
import com.coding.core.repository.KnowledgeBaseRepository;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 知识库Repository实现类
 * @author weilong
 */
@DS("postgresql")
@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public Boolean save(KnowledgeBaseDO knowledgeBase) {
        if (knowledgeBase.getCreateTime() == null) {
            knowledgeBase.setCreateTime(LocalDateTime.now());
        }
        if (knowledgeBase.getUpdateTime() == null) {
            knowledgeBase.setUpdateTime(LocalDateTime.now());
        }
        if (knowledgeBase.getDeleted() == null) {
            knowledgeBase.setDeleted(0);
        }
        if (knowledgeBase.getVectorCount() == null) {
            knowledgeBase.setVectorCount(0L);
        }
        return knowledgeBaseMapper.insert(knowledgeBase) > 0;
    }

    @Override
    public Boolean update(KnowledgeBaseDO knowledgeBase) {
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        return knowledgeBaseMapper.updateById(knowledgeBase) > 0;
    }

    @Override
    public Boolean deleteById(Long id) {
        LambdaUpdateWrapper<KnowledgeBaseDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getId, id)
                .set(KnowledgeBaseDO::getDeleted, 1)
                .set(KnowledgeBaseDO::getUpdateTime, LocalDateTime.now());
        return knowledgeBaseMapper.update(null, wrapper) > 0;
    }

    @Override
    public KnowledgeBaseDO getById(Long id) {
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getId, id)
                .eq(KnowledgeBaseDO::getDeleted, 0);
        return knowledgeBaseMapper.selectOne(wrapper);
    }

    @Override
    public IPage<KnowledgeBaseDO> page(KnowledgeBasePageRequest request) {
        Page<KnowledgeBaseDO> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getDeleted, 0);
        
        if (StringUtils.isNotBlank(request.getName())) {
            wrapper.like(KnowledgeBaseDO::getName, request.getName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(KnowledgeBaseDO::getStatus, request.getStatus());
        }
        if (request.getUserId() != null) {
            wrapper.eq(KnowledgeBaseDO::getUserId, request.getUserId());
        }
        
        wrapper.orderByDesc(KnowledgeBaseDO::getCreateTime);
        
        return knowledgeBaseMapper.selectPage(page, wrapper);
    }

    @Override
    public Boolean increaseVectorCount(Long id, Long count) {
        return knowledgeBaseMapper.increaseVectorCount(id, count) > 0;
    }

    @Override
    public Boolean decreaseVectorCount(Long id, Long count) {
        return knowledgeBaseMapper.decreaseVectorCount(id, count) > 0;
    }
}

