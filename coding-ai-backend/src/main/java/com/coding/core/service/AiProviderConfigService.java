package com.coding.core.service;

import com.coding.core.model.entity.AiProviderConfigDO;

/**
 * 提供商配置服务接口
 */
public interface AiProviderConfigService {

    /**
     * 根据提供商代码和服务类型获取配置
     */
    AiProviderConfigDO getByProviderCodeAndServiceType(String providerCode, String serviceType);
}
