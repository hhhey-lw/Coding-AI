package com.coding.admin.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.coding.admin.config.properties.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 */
@Configuration
@RequiredArgsConstructor
public class AliyunOssConfig {

    private final OssProperties ossProperties;

    /**
     * 创建OSS客户端Bean
     */
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(
                ossProperties.getOss().getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }
}
