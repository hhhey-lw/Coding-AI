package com.coding.admin.service;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeService {

    /**
     * 根据文件类型将文件向量化到知识库中。
     *
     * @param file 上传到文件
     * @return 是否上传成功
     */
    Boolean loadFileByType(MultipartFile file);

    /**
     * 相似性搜索
     *
     * @param query 查询内容
     * @param topK 返回的相似性文档数量
     * @return
     */
    List<Document> similaritySearch(String query, int topK);

}
