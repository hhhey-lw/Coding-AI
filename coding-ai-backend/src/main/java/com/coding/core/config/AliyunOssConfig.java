package com.coding.core.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * OSS配置类
 */
@Configuration
public class AliyunOssConfig {

    /**
     * 创建OSS客户端Bean
     */
    @Bean
    public OSS ossClient(OssProperties ossProperties) {
        return new OSSClientBuilder().build(
                ossProperties.getOss().getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }

    /**
     * 阿里云OSS配置属性
     */
    @Data
    @Component
    @ConfigurationProperties(prefix = "aliyun")
    public static class OssProperties {

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

}
