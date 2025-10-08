package com.coding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.admin.mapper.ProviderConfigMapper;
import com.coding.admin.model.entity.ProviderConfigDO;
import com.coding.admin.service.ProviderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 提供商配置服务实现
 */
@Service
@RequiredArgsConstructor
public class ProviderConfigServiceImpl implements ProviderConfigService {

    private final ProviderConfigMapper providerConfigMapper;

    @Override
    public ProviderConfigDO getByProviderCodeAndServiceType(String providerCode, String serviceType) {
        LambdaQueryWrapper<ProviderConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProviderConfigDO::getProviderCode, providerCode)
               .eq(ProviderConfigDO::getServiceType, serviceType)
               .eq(ProviderConfigDO::getStatus, Boolean.TRUE);
        return providerConfigMapper.selectOne(wrapper);
    }
}
