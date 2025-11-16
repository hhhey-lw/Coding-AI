package com.coding.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.model.request.KnowledgeBaseAddRequest;
import com.coding.core.model.request.KnowledgeBasePageRequest;
import com.coding.core.model.request.KnowledgeBaseUpdateRequest;
import com.coding.core.model.vo.KnowledgeBaseVO;

/**
 * 知识库管理Service
 * @author weilong
 */
public interface KnowledgeBaseMgmtService {

    /**
     * 新增知识库
     * @param request 新增请求
     * @return 知识库ID
     */
    Long addKnowledgeBase(KnowledgeBaseAddRequest request);

    /**
     * 更新知识库
     * @param request 更新请求
     * @return 是否成功
     */
    Boolean updateKnowledgeBase(KnowledgeBaseUpdateRequest request);

    /**
     * 删除知识库
     * @param id 知识库ID
     * @return 是否成功
     */
    Boolean deleteKnowledgeBase(Long id);

    /**
     * 根据ID查询知识库
     * @param id 知识库ID
     * @return 知识库信息
     */
    KnowledgeBaseVO getKnowledgeBase(Long id);

    /**
     * 分页查询知识库
     * @param request 分页请求
     * @return 分页结果
     */
    IPage<KnowledgeBaseVO> pageKnowledgeBase(KnowledgeBasePageRequest request);

}

