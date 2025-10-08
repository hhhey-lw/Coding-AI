package com.coding.admin.controller;

import com.coding.admin.common.Result;
import com.coding.admin.model.model.FileUploadResponseModel;
import com.coding.admin.service.FileUploadService;
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

    private final FileUploadService fileUploadService;

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片", description = "支持jpg、jpeg、png、gif、webp、bmp格式，最大10MB")
    public Result<FileUploadResponseModel> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("接收到图片上传请求，文件名: {}, 大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        FileUploadResponseModel response = fileUploadService.uploadImage(file);

        log.info("图片上传成功，URL: {}", response.getFileUrl());
        return Result.success(response);
    }
}
