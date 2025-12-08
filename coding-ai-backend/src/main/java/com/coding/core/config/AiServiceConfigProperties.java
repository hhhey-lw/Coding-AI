package com.coding.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * AI 服务配置属性
 */
@Data
@Configuration
@PropertySource(value = "classpath:ai-service-config.yaml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "ai-service")
public class AiServiceConfigProperties {

    /**
     * AI 提供商配置列表
     */
    private List<ProviderConfig> providers;

    /**
     * MCP 服务器配置列表
     */
    private List<McpServerConfig> mcpServers;

    /**
     * AI 模型配置列表
     */
    private List<ModelConfig> models;

    /**
     * 提供商配置
     */
    @Data
    public static class ProviderConfig {
        private String providerCode;
        private String providerName;
        private String serviceType;
        private String baseUrl;
        private String authorization;
        private Boolean status;
    }

    /**
     * MCP 服务器配置
     */
    @Data
    public static class McpServerConfig {
        private String providerCode;
        private String serverName;
        private String serverCode;
        private String endpoint;
        private String toolName;
        private List<ToolParam> toolParams;
        private String contentDescription;
        private Boolean status;
    }

    /**
     * 工具参数
     */
    @Data
    public static class ToolParam {
        private String key;
        private String desc;
        private String type;
    }

    /**
     * 模型配置
     */
    @Data
    public static class ModelConfig {
        private String providerCode;
        private String providerName;
        private String modelType;
        private String modelId;
        private Boolean status;
    }
}
