package com.coding.admin.repository.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.mapper.postgresql.KnowledgeVectorMapper;
import com.coding.admin.model.entity.KnowledgeVectorDO;
import com.coding.admin.model.request.KnowledgeVectorPageRequest;
import com.coding.admin.repository.KnowledgeVectorRepository;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识向量Repository实现类
 * 使用PostgreSQL数据源
 * @author weilong
 */
@DS("postgresql")
@Repository
public class KnowledgeVectorRepositoryImpl implements KnowledgeVectorRepository {

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Override
    public Boolean save(KnowledgeVectorDO vector) {
        if (vector.getCreateTime() == null) {
            vector.setCreateTime(LocalDateTime.now());
        }
        if (vector.getUpdateTime() == null) {
            vector.setUpdateTime(LocalDateTime.now());
        }
        if (vector.getDeleted() == null) {
            vector.setDeleted(0);
        }
        return knowledgeVectorMapper.insert(vector) > 0;
    }

    @Override
    public Boolean saveBatch(List<KnowledgeVectorDO> vectors) {
        for (KnowledgeVectorDO vector : vectors) {
            if (vector.getCreateTime() == null) {
                vector.setCreateTime(LocalDateTime.now());
            }
            if (vector.getUpdateTime() == null) {
                vector.setUpdateTime(LocalDateTime.now());
            }
            if (vector.getDeleted() == null) {
                vector.setDeleted(0);
            }
            knowledgeVectorMapper.insert(vector);
        }
        return true;
    }

    @Override
    public Boolean update(KnowledgeVectorDO vector) {
        vector.setUpdateTime(LocalDateTime.now());
        return knowledgeVectorMapper.updateById(vector) > 0;
    }

    @Override
    public Boolean deleteById(String id) {
        LambdaUpdateWrapper<KnowledgeVectorDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeVectorDO::getId, id)
                .set(KnowledgeVectorDO::getDeleted, 1)
                .set(KnowledgeVectorDO::getUpdateTime, LocalDateTime.now());
        return knowledgeVectorMapper.update(null, wrapper) > 0;
    }

    @Override
    public Boolean deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaUpdateWrapper<KnowledgeVectorDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeVectorDO::getKnowledgeBaseId, knowledgeBaseId)
                .set(KnowledgeVectorDO::getDeleted, 1)
                .set(KnowledgeVectorDO::getUpdateTime, LocalDateTime.now());
        return knowledgeVectorMapper.update(null, wrapper) > 0;
    }

    @Override
    public KnowledgeVectorDO getById(String id) {
        LambdaQueryWrapper<KnowledgeVectorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVectorDO::getId, id)
                .eq(KnowledgeVectorDO::getDeleted, 0);
        return knowledgeVectorMapper.selectOne(wrapper);
    }

    @Override
    public IPage<KnowledgeVectorDO> page(KnowledgeVectorPageRequest request) {
        Page<KnowledgeVectorDO> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<KnowledgeVectorDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeVectorDO::getDeleted, 0);
        wrapper.eq(KnowledgeVectorDO::getKnowledgeBaseId, request.getKnowledgeBaseId());
        
        if (StringUtils.isNotBlank(request.getFileName())) {
            wrapper.like(KnowledgeVectorDO::getFileName, request.getFileName());
        }
        if (StringUtils.isNotBlank(request.getFileType())) {
            wrapper.eq(KnowledgeVectorDO::getFileType, request.getFileType());
        }
        
        wrapper.orderByDesc(KnowledgeVectorDO::getCreateTime);
        
        return knowledgeVectorMapper.selectPage(page, wrapper);
    }

    @Override
    public Long countByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeVectorMapper.countByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public List<KnowledgeVectorDO> similaritySearch(Long knowledgeBaseId, String embedding, Integer topK) {
        return knowledgeVectorMapper.similaritySearch(knowledgeBaseId, embedding, topK);
    }
}

