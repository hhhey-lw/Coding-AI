package com.coding.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.admin.model.request.KnowledgeVectorAddRequest;
import com.coding.admin.model.request.KnowledgeVectorPageRequest;
import com.coding.admin.model.request.KnowledgeVectorUpdateRequest;
import com.coding.admin.model.vo.KnowledgeVectorVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识向量管理Service
 * @author weilong
 */
public interface KnowledgeVectorMgmtService {

    /**
     * 新增向量
     * @param request 新增请求
     * @return 向量ID
     */
    String addVector(KnowledgeVectorAddRequest request);

    /**
     * 更新向量
     * @param request 更新请求
     * @return 是否成功
     */
    Boolean updateVector(KnowledgeVectorUpdateRequest request);

    /**
     * 删除向量
     * @param id 向量ID
     * @return 是否成功
     */
    Boolean deleteVector(String id);

    /**
     * 根据ID查询向量
     * @param id 向量ID
     * @return 向量信息
     */
    KnowledgeVectorVO getVector(String id);

    /**
     * 分页查询向量
     * @param request 分页请求
     * @return 分页结果
     */
    IPage<KnowledgeVectorVO> pageVector(KnowledgeVectorPageRequest request);


    /**
     * 根据文件类型将文件向量化到知识库中。
     *
     * @param file 上传到文件
     * @return 是否上传成功
     */
    Boolean loadFileByType(Long knowledgeBaseId, MultipartFile file);

    /**
     * 相似性搜索
     * @param knowledgeBaseId 知识库ID
     * @param query 查询内容
     * @param topK 返回数量
     * @return 相似向量列表
     */
    List<KnowledgeVectorVO> similaritySearch(Long knowledgeBaseId, String query, Integer topK);
}

