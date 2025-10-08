package com.coding.admin.service;

import com.coding.admin.model.entity.ProviderConfigDO;

/**
 * 提供商配置服务接口
 */
public interface ProviderConfigService {

    /**
     * 根据提供商代码和服务类型获取配置
     */
    ProviderConfigDO getByProviderCodeAndServiceType(String providerCode, String serviceType);
}
