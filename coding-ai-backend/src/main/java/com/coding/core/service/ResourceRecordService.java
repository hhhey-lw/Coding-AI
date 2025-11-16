package com.coding.core.service;

import com.coding.core.model.model.ResourceUploadResponseModel;
import org.springframework.web.multipart.MultipartFile;

public interface ResourceRecordService {

    /**
     * 上传图片到OSS
     *
     * @param file 上传的文件
     * @return 文件上传响应
     */
    ResourceUploadResponseModel uploadImage(MultipartFile file);

}
