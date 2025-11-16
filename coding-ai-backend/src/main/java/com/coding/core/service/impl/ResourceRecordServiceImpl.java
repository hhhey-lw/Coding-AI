package com.coding.core.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.coding.core.config.AliyunOssConfig;
import com.coding.core.enums.ImageFormatEnum;
import com.coding.core.model.entity.ResourceRecordDO;
import com.coding.core.model.model.ResourceUploadResponseModel;
import com.coding.core.repository.ResourceRecordRepository;
import com.coding.core.service.ResourceRecordService;
import com.coding.core.utils.Md5Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件上传服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRecordServiceImpl implements ResourceRecordService {

    private final OSS ossClient;
    private final AliyunOssConfig.OssProperties ossProperties;
    private final ResourceRecordRepository resourceRecordRepository;

    /**
     * 最大文件大小：10MB
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 上传图片到OSS
     *
     * @param file 上传的文件
     * @return 文件上传响应
     */
    @Override
    public ResourceUploadResponseModel uploadImage(MultipartFile file) {
        // 验证文件
        validateImageFile(file);

        // 计算文件MD5
        String fileMd5 = Md5Utils.calculateMd5(file);
        log.info("文件MD5: {}", fileMd5);

        // 检查文件是否已存在
        ResourceRecordDO existingFile = resourceRecordRepository.getByMd5(fileMd5);
        if (existingFile != null) {
            log.info("文件已存在，直接返回URL: {}", existingFile.getFileUrl());
            return buildResponseFromFileInfo(existingFile);
        }

        try {
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());

            // 上传到OSS
            String objectKey = "images/" + fileName;
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getOss().getBucketName(),
                    objectKey,
                    file.getInputStream()
            );

            PutObjectResult result = ossClient.putObject(putObjectRequest);
            log.info("文件上传成功，ETag: {}", result.getETag());

            // 构建文件URL
            String fileUrl = buildFileUrl(objectKey);

            // 保存文件信息到数据库
            ResourceRecordDO resourceRecordDO = buildFileInfo(file, fileName, fileUrl, fileMd5);
            Long fileId = resourceRecordRepository.save(resourceRecordDO);
            if (fileId == null) {
                log.warn("文件信息保存失败，但文件已上传成功");
            }

            // 构建并返回响应
            return buildUploadResponse(fileName, fileUrl, file);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 构建文件上传响应
     *
     * @param fileName 文件名
     * @param fileUrl 文件URL
     * @param file 原始文件
     * @return 文件上传响应
     */
    private ResourceUploadResponseModel buildUploadResponse(String fileName, String fileUrl, MultipartFile file) {
        ResourceUploadResponseModel response = new ResourceUploadResponseModel();
        response.setFileName(fileName);
        response.setFileUrl(fileUrl);
        response.setFileSize(file.getSize());
        response.setContentType(file.getContentType());
        response.setUploadTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 构建FileInfo对象
     *
     * @param file 上传的文件
     * @param fileName 生成的文件名
     * @param fileUrl 文件URL
     * @param fileMd5 文件MD5值
     * @return FileInfo对象
     */
    private ResourceRecordDO buildFileInfo(MultipartFile file, String fileName, String fileUrl, String fileMd5) {
        ResourceRecordDO resourceRecordDO = new ResourceRecordDO();
        resourceRecordDO.setFileName(fileName);
        resourceRecordDO.setOriginalName(file.getOriginalFilename());
        resourceRecordDO.setFileUrl(fileUrl);
        resourceRecordDO.setFileSize(file.getSize());
        resourceRecordDO.setFileMd5(fileMd5);
        resourceRecordDO.setContentType(file.getContentType());
        resourceRecordDO.setCreateTime(LocalDateTime.now());
        return resourceRecordDO;
    }

    /**
     * 从文件信息构建响应
     *
     * @param resourceRecordDO 文件信息
     * @return 文件上传响应
     */
    private ResourceUploadResponseModel buildResponseFromFileInfo(ResourceRecordDO resourceRecordDO) {
        ResourceUploadResponseModel response = new ResourceUploadResponseModel();
        response.setFileName(resourceRecordDO.getFileName());
        response.setFileUrl(resourceRecordDO.getFileUrl());
        response.setFileSize(resourceRecordDO.getFileSize());
        response.setContentType(resourceRecordDO.getContentType());
        response.setUploadTime(System.currentTimeMillis());
        return response;
    }

    /**
     * 验证图片文件
     *
     * @param file 上传的文件
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (!ImageFormatEnum.isSupportedMimeType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持: " + ImageFormatEnum.getAllMimeTypes());
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ImageFormatEnum.isSupportedExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名，仅支持: " + ImageFormatEnum.getAllExtensions());
        }
    }

    /**
     * 生成唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 生成的文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("文件必须有扩展名");
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 构建文件URL
     *
     * @param objectKey OSS对象键
     * @return 文件URL
     */
    private String buildFileUrl(String objectKey) {
        // 构建公网访问URL
        String endpoint = ossProperties.getOss().getEndpoint();
        String bucketName = ossProperties.getOss().getBucketName();

        // 如果endpoint包含http://或https://，直接使用
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint.replace("://", "://" + bucketName + ".") + "/" + objectKey;
        } else {
            return "https://" + bucketName + "." + endpoint + "/" + objectKey;
        }
    }
}
