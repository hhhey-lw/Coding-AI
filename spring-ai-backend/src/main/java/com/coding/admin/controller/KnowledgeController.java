package com.coding.admin.controller;

import com.coding.admin.common.Result;
import com.coding.admin.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "知识库管理")
@RequestMapping("/ai/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @Operation(summary = "上传文件到知识库")
    @PostMapping("/upload-file")
    public Result<Boolean> uploadFileToVectorStore(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        return Result.success(knowledgeService.loadFileByType(file));
    }

}
