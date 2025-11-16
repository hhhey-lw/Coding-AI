package com.coding.core.model.model;

import lombok.Data;

/**
 * 文件上传响应DTO
 */
@Data
public class ResourceUploadResponseModel {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 上传时间戳
     */
    private Long uploadTime;
}
