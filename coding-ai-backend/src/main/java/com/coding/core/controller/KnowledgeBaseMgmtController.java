package com.coding.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.common.Result;
import com.coding.core.model.request.KnowledgeBaseAddRequest;
import com.coding.core.model.request.KnowledgeBasePageRequest;
import com.coding.core.model.request.KnowledgeBaseUpdateRequest;
import com.coding.core.model.vo.KnowledgeBaseVO;
import com.coding.core.service.KnowledgeBaseMgmtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库管理Controller
 * @author weilong
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-base")
@Tag(name = "知识库管理", description = "知识库CRUD操作")
public class KnowledgeBaseMgmtController {

    @Resource
    private KnowledgeBaseMgmtService knowledgeBaseMgmtService;

    @PostMapping("/add")
    @Operation(summary = "新增知识库")
    public Result<Long> addKnowledgeBase(@Valid @RequestBody KnowledgeBaseAddRequest request) {
        Long id = knowledgeBaseMgmtService.addKnowledgeBase(request);
        return Result.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    public Result<Boolean> updateKnowledgeBase(@Valid @RequestBody KnowledgeBaseUpdateRequest request) {
        Boolean result = knowledgeBaseMgmtService.updateKnowledgeBase(request);
        return Result.success(result);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除知识库")
    public Result<Boolean> deleteKnowledgeBase(@PathVariable Long id) {
        Boolean result = knowledgeBaseMgmtService.deleteKnowledgeBase(id);
        return Result.success(result);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "查询知识库详情")
    public Result<KnowledgeBaseVO> getKnowledgeBase(@PathVariable Long id) {
        KnowledgeBaseVO vo = knowledgeBaseMgmtService.getKnowledgeBase(id);
        return Result.success(vo);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询知识库")
    public Result<IPage<KnowledgeBaseVO>> pageKnowledgeBase(@Valid @RequestBody KnowledgeBasePageRequest request) {
        IPage<KnowledgeBaseVO> page = knowledgeBaseMgmtService.pageKnowledgeBase(request);
        return Result.success(page);
    }
}

