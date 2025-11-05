package com.coding.admin.service.impl;

import com.coding.admin.service.KnowledgeService;
import com.coding.workflow.exception.BizException;
import com.coding.workflow.utils.AssertUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 知识库服务实现类
 * @author weilong
 */
@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private VectorStore vectorStore;

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean loadFileByType(MultipartFile file) {
        AssertUtil.isNotNull(file, "上传的文件不能为空");
        log.info("开始处理文件上传：fileName={}, fileSize={}", file.getOriginalFilename(), file.getSize());
        try {
            // 1. 创建临时文件
            Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 2. 读取文件内容
            List<Document> documents;
            // 读取不同类型的文件内容
            String fileName = file.getOriginalFilename();
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
            vectorStore.add(documents);

            // 4. 删除临时文件
            Files.deleteIfExists(tempFile);

            log.info("文件处理完成: fileName={}, documentsCount={}", fileName,  documents.size());

            return true;

        } catch (IOException e) {
            log.error("文件处理失败: fileName={}, error={}", file.getOriginalFilename(),  e.getMessage(), e);
            throw new BizException("上传文件至知识库失败：" + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Document> similaritySearch(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(request);
    }

}
