package com.coding.core.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.model.entity.KnowledgeBaseDO;
import com.coding.core.model.request.KnowledgeBaseAddRequest;
import com.coding.core.model.request.KnowledgeBasePageRequest;
import com.coding.core.model.request.KnowledgeBaseUpdateRequest;
import com.coding.core.model.vo.KnowledgeBaseVO;
import com.coding.core.repository.KnowledgeBaseRepository;
import com.coding.core.repository.KnowledgeVectorRepository;
import com.coding.core.service.KnowledgeBaseMgmtService;
import com.coding.core.utils.UserContextHolder;
import com.coding.workflow.utils.AssertUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库管理Service实现类
 * 使用PostgreSQL数据源
 * @author weilong
 */
@DS("postgresql")
@Slf4j
@Service
public class KnowledgeBaseMgmtServiceImpl implements KnowledgeBaseMgmtService {

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private KnowledgeVectorRepository knowledgeVectorRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addKnowledgeBase(KnowledgeBaseAddRequest request) {
        Long userId = UserContextHolder.getUserId();
        
        KnowledgeBaseDO knowledgeBase = KnowledgeBaseDO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(userId)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .vectorCount(0L)
                .build();
        
        Boolean success = knowledgeBaseRepository.save(knowledgeBase);
        AssertUtil.isTrue(success, "创建知识库失败");
        
        log.info("创建知识库成功: id={}, name={}, userId={}", knowledgeBase.getId(), knowledgeBase.getName(), userId);
        return knowledgeBase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateKnowledgeBase(KnowledgeBaseUpdateRequest request) {
        KnowledgeBaseDO existingKb = knowledgeBaseRepository.getById(request.getId());
        AssertUtil.isNotNull(existingKb, "知识库不存在");
        
        KnowledgeBaseDO knowledgeBase = KnowledgeBaseDO.builder()
                .id(request.getId())
                .build();
        
        if (request.getName() != null) {
            knowledgeBase.setName(request.getName());
        }
        if (request.getDescription() != null) {
            knowledgeBase.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            knowledgeBase.setStatus(request.getStatus());
        }
        
        Boolean success = knowledgeBaseRepository.update(knowledgeBase);
        AssertUtil.isTrue(success, "更新知识库失败");
        
        log.info("更新知识库成功: id={}", request.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteKnowledgeBase(Long id) {
        KnowledgeBaseDO existingKb = knowledgeBaseRepository.getById(id);
        AssertUtil.isNotNull(existingKb, "知识库不存在");
        
        // 删除知识库下的所有向量
        knowledgeVectorRepository.deleteByKnowledgeBaseId(id);
        
        // 删除知识库
        Boolean success = knowledgeBaseRepository.deleteById(id);
        AssertUtil.isTrue(success, "删除知识库失败");
        
        log.info("删除知识库成功: id={}", id);
        return true;
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBase(Long id) {
        KnowledgeBaseDO knowledgeBase = knowledgeBaseRepository.getById(id);
        AssertUtil.isNotNull(knowledgeBase, "知识库不存在");
        
        return convertToVO(knowledgeBase);
    }

    @Override
    public IPage<KnowledgeBaseVO> pageKnowledgeBase(KnowledgeBasePageRequest request) {
        IPage<KnowledgeBaseDO> page = knowledgeBaseRepository.page(request);
        return page.convert(this::convertToVO);
    }

    /**
     * 转换为VO
     */
    private KnowledgeBaseVO convertToVO(KnowledgeBaseDO knowledgeBase) {
        return KnowledgeBaseVO.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .userId(knowledgeBase.getUserId())
                .status(knowledgeBase.getStatus())
                .vectorCount(knowledgeBase.getVectorCount())
                .createTime(knowledgeBase.getCreateTime())
                .updateTime(knowledgeBase.getUpdateTime())
                .build();
    }
}

