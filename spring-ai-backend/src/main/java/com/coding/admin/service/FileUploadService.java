package com.coding.admin.service;

import com.coding.admin.model.model.FileUploadResponseModel;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 上传图片到OSS
     *
     * @param file 上传的文件
     * @return 文件上传响应
     */
    FileUploadResponseModel uploadImage(MultipartFile file);

}
