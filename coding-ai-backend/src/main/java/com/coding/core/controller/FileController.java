package com.coding.core.controller;

import com.coding.core.common.Result;
import com.coding.core.model.model.ResourceUploadResponseModel;
import com.coding.core.service.ResourceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final ResourceRecordService resourceRecordService;

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片", description = "支持jpg、jpeg、png、gif、webp、bmp格式，最大10MB")
    public Result<ResourceUploadResponseModel> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("接收到图片上传请求，文件名: {}, 大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        ResourceUploadResponseModel response = resourceRecordService.uploadImage(file);

        log.info("图片上传成功，URL: {}", response.getFileUrl());
        return Result.success(response);
    }
}
