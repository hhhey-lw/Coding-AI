package com.coding.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.common.Result;
import com.coding.core.model.request.KnowledgeVectorAddRequest;
import com.coding.core.model.request.KnowledgeVectorPageRequest;
import com.coding.core.model.request.KnowledgeVectorUpdateRequest;
import com.coding.core.model.vo.KnowledgeVectorVO;
import com.coding.core.service.KnowledgeVectorMgmtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识向量管理Controller
 * @author weilong
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-vector")
@Tag(name = "知识向量管理", description = "知识向量CRUD操作")
public class KnowledgeVectorMgmtController {

    @Resource
    private KnowledgeVectorMgmtService knowledgeVectorMgmtService;

    @PostMapping("/add")
    @Operation(summary = "新增向量")
    public Result<String> addVector(@Valid @RequestBody KnowledgeVectorAddRequest request) {
        String id = knowledgeVectorMgmtService.addVector(request);
        return Result.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新向量")
    public Result<Boolean> updateVector(@Valid @RequestBody KnowledgeVectorUpdateRequest request) {
        Boolean result = knowledgeVectorMgmtService.updateVector(request);
        return Result.success(result);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除向量")
    public Result<Boolean> deleteVector(@PathVariable String id) {
        Boolean result = knowledgeVectorMgmtService.deleteVector(id);
        return Result.success(result);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "查询向量详情")
    public Result<KnowledgeVectorVO> getVector(@PathVariable String id) {
        KnowledgeVectorVO vo = knowledgeVectorMgmtService.getVector(id);
        return Result.success(vo);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询向量")
    public Result<IPage<KnowledgeVectorVO>> pageVector(@Valid @RequestBody KnowledgeVectorPageRequest request) {
        IPage<KnowledgeVectorVO> page = knowledgeVectorMgmtService.pageVector(request);
        return Result.success(page);
    }

    @Operation(summary = "上传文件到知识库")
    @PostMapping("/upload-file")
    public Result<Boolean> uploadFileToVectorStore(
            @RequestParam Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        return Result.success(knowledgeVectorMgmtService.loadFileByType(knowledgeBaseId, file));
    }

    @GetMapping("/similarity-search")
    @Operation(summary = "相似性搜索")
    public Result<List<KnowledgeVectorVO>> similaritySearch(
            @RequestParam Long knowledgeBaseId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK) {
        List<KnowledgeVectorVO> result = knowledgeVectorMgmtService.similaritySearch(knowledgeBaseId, query, topK);
        return Result.success(result);
    }
}

