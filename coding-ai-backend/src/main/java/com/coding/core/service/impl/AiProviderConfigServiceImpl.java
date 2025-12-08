package com.coding.core.service.impl;

import com.coding.core.config.AiServiceConfigProperties;
import com.coding.core.service.AiProviderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 提供商配置服务实现
 */
@Service
@RequiredArgsConstructor
public class AiProviderConfigServiceImpl implements AiProviderConfigService {

    private final AiServiceConfigProperties configProperties;

    @Override
    public AiServiceConfigProperties.ProviderConfig getByProviderCodeAndServiceType(String providerCode, String serviceType) {
        return configProperties.getProviders().stream()
                .filter(config -> config.getProviderCode().equals(providerCode))
                .filter(config -> config.getServiceType().equals(serviceType))
                .filter(config -> Boolean.TRUE.equals(config.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
