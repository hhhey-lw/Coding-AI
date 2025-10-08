package com.coding.admin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun")
public class OssProperties {

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;

    /**
     * OSS配置
     */
    private OssConfig oss;

    @Data
    public static class OssConfig {
        /**
         * OSS服务端点
         */
        private String endpoint;

        /**
         * 存储桶名称
         */
        private String bucketName;
    }
}
