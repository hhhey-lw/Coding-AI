package com.coding.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.core.mapper.AiProviderConfigMapper;
import com.coding.core.model.entity.AiProviderConfigDO;
import com.coding.core.service.AiProviderConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 提供商配置服务实现
 */
@Service
@RequiredArgsConstructor
public class AiProviderConfigServiceImpl implements AiProviderConfigService {

    private final AiProviderConfigMapper aiProviderConfigMapper;

    @Override
    public AiProviderConfigDO getByProviderCodeAndServiceType(String providerCode, String serviceType) {
        LambdaQueryWrapper<AiProviderConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProviderConfigDO::getProviderCode, providerCode)
               .eq(AiProviderConfigDO::getServiceType, serviceType)
               .eq(AiProviderConfigDO::getStatus, Boolean.TRUE);
        return aiProviderConfigMapper.selectOne(wrapper);
    }
}
