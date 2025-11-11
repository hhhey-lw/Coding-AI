package com.coding.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.admin.model.entity.KnowledgeVectorDO;
import com.coding.admin.model.request.KnowledgeVectorAddRequest;
import com.coding.admin.model.request.KnowledgeVectorPageRequest;
import com.coding.admin.model.request.KnowledgeVectorUpdateRequest;
import com.coding.admin.model.vo.KnowledgeVectorVO;
import com.coding.admin.repository.KnowledgeBaseRepository;
import com.coding.admin.repository.KnowledgeVectorRepository;
import com.coding.admin.service.KnowledgeVectorMgmtService;
import com.coding.workflow.exception.BizException;
import com.coding.workflow.utils.AssertUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识向量管理Service实现类
 * 使用PostgreSQL数据源
 * @author weilong
 */
@DS("postgresql")
@Slf4j
@Service
public class KnowledgeVectorMgmtServiceImpl implements KnowledgeVectorMgmtService {

    @Resource
    private KnowledgeVectorRepository knowledgeVectorRepository;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private EmbeddingModel embeddingModel;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addVector(KnowledgeVectorAddRequest request) {
        // 生成向量嵌入
        String embedding = generateEmbedding(request.getContent());
        
        KnowledgeVectorDO vector = KnowledgeVectorDO.builder()
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .embedding(embedding)
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .build();
        
        Boolean success = knowledgeVectorRepository.save(vector);
        AssertUtil.isTrue(success, "创建向量失败");
        
        // 增加知识库的向量数量
        knowledgeBaseRepository.increaseVectorCount(request.getKnowledgeBaseId(), 1L);
        
        log.info("创建向量成功: id={}, knowledgeBaseId={}", vector.getId(), request.getKnowledgeBaseId());
        return vector.getId();
    }

    /**
     * 将文件加载到指定知识库中
     * @param file 文件
     * @param knowledgeBaseId 知识库ID，如果为null则使用默认的vectorStore
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean loadFileByType(Long knowledgeBaseId, MultipartFile file) {
        AssertUtil.isNotNull(file, "上传的文件不能为空");
        log.info("开始处理文件上传：fileName={}, fileSize={}, knowledgeBaseId={}",
                file.getOriginalFilename(), file.getSize(), knowledgeBaseId);
        try {
            // 1. 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 2. 读取文件内容
            List<Document> documents;
            // 读取不同类型的文件内容
            String fileName = file.getOriginalFilename();
            String fileType = getFileType(fileName);

            if (fileName.toLowerCase().endsWith("pdf")) {
                // 读取PDF文件内容
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(tempFile.toUri().toString());
                documents = pdfReader.get();
            } else {
                // 处理其他文件类型
                TikaDocumentReader tikaReader = new TikaDocumentReader(tempFile.toUri().toString());
                documents = tikaReader.get();
            }

            // 3. 将内容存储到向量数据库
            saveToKnowledgeBase(documents, knowledgeBaseId, fileName, fileType);

            // 4. 删除临时文件
            Files.deleteIfExists(tempFile);

            log.info("文件处理完成: fileName={}, documentsCount={}, knowledgeBaseId={}",
                    fileName, documents.size(), knowledgeBaseId);

            return true;

        } catch (IOException e) {
            log.error("文件处理失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new BizException("上传文件至知识库失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateVector(KnowledgeVectorUpdateRequest request) {
        KnowledgeVectorDO existingVector = knowledgeVectorRepository.getById(request.getId());
        AssertUtil.isNotNull(existingVector, "向量不存在");
        
        KnowledgeVectorDO vector = KnowledgeVectorDO.builder()
                .id(request.getId())
                .build();
        
        if (request.getContent() != null) {
            vector.setContent(request.getContent());
            // 重新生成向量嵌入
            String embedding = generateEmbedding(request.getContent());
            vector.setEmbedding(embedding);
        }
        if (request.getMetadata() != null) {
            vector.setMetadata(request.getMetadata());
        }
        if (request.getFileName() != null) {
            vector.setFileName(request.getFileName());
        }
        if (request.getFileType() != null) {
            vector.setFileType(request.getFileType());
        }
        
        Boolean success = knowledgeVectorRepository.update(vector);
        AssertUtil.isTrue(success, "更新向量失败");
        
        log.info("更新向量成功: id={}", request.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteVector(String id) {
        KnowledgeVectorDO existingVector = knowledgeVectorRepository.getById(id);
        AssertUtil.isNotNull(existingVector, "向量不存在");
        
        Boolean success = knowledgeVectorRepository.deleteById(id);
        AssertUtil.isTrue(success, "删除向量失败");
        
        // 减少知识库的向量数量
        knowledgeBaseRepository.decreaseVectorCount(existingVector.getKnowledgeBaseId(), 1L);
        
        log.info("删除向量成功: id={}", id);
        return true;
    }

    @Override
    public KnowledgeVectorVO getVector(String id) {
        KnowledgeVectorDO vector = knowledgeVectorRepository.getById(id);
        AssertUtil.isNotNull(vector, "向量不存在");
        
        return convertToVO(vector);
    }

    @Override
    public IPage<KnowledgeVectorVO> pageVector(KnowledgeVectorPageRequest request) {
        IPage<KnowledgeVectorDO> page = knowledgeVectorRepository.page(request);
        return page.convert(this::convertToVO);
    }

    @Override
    public List<KnowledgeVectorVO> similaritySearch(Long knowledgeBaseId, String query, Integer topK) {
        // 生成查询向量
        String queryEmbedding = generateEmbedding(query);
        
        // 执行相似性搜索
        List<KnowledgeVectorDO> vectors = knowledgeVectorRepository.similaritySearch(
                knowledgeBaseId, queryEmbedding, topK);
        
        return vectors.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将文档保存到指定知识库
     */
    private void saveToKnowledgeBase(List<Document> documents, Long knowledgeBaseId, String fileName, String fileType) {
        List<KnowledgeVectorDO> vectors = new ArrayList<>();

        for (Document doc : documents) {
            // 生成向量嵌入
            String embedding = generateEmbedding(doc.getFormattedContent());

            KnowledgeVectorDO vector = KnowledgeVectorDO.builder()
                    .knowledgeBaseId(knowledgeBaseId)
                    .content(doc.getFormattedContent())
                    .metadata(doc.getMetadata() != null ? doc.getMetadata().toString() : null)
                    .embedding(embedding)
                    .fileName(fileName)
                    .fileType(fileType)
                    .build();

            vectors.add(vector);
        }

        // 批量保存
        knowledgeVectorRepository.saveBatch(vectors);

        // 更新知识库的向量数量
        knowledgeBaseRepository.increaseVectorCount(knowledgeBaseId, (long) vectors.size());
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 生成向量嵌入
     * 返回PostgreSQL pgvector格式：[0.1,0.2,0.3,...]
     */
    private String generateEmbedding(String content) {
        try {
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(content), null));
            float[] embedding = response.getResult().getOutput();
            
            // 将float数组转换为PostgreSQL pgvector格式 [0.1,0.2,0.3,...]
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(embedding[i]);
            }
            sb.append("]");
            
            return sb.toString();
        } catch (Exception e) {
            log.error("生成向量嵌入失败: content length={}", content != null ? content.length() : 0, e);
            throw new RuntimeException("生成向量嵌入失败: " + e.getMessage());
        }
    }

    /**
     * 转换为VO
     */
    private KnowledgeVectorVO convertToVO(KnowledgeVectorDO vector) {
        return KnowledgeVectorVO.builder()
                .id(vector.getId())
                .knowledgeBaseId(vector.getKnowledgeBaseId())
                .content(vector.getContent())
                .metadata(vector.getMetadata())
                .fileName(vector.getFileName())
                .fileType(vector.getFileType())
                .createTime(vector.getCreateTime())
                .updateTime(vector.getUpdateTime())
                .build();
    }
}

