package com.coding.core.service.impl;

import com.coding.core.config.AiServiceConfigProperties;
import com.coding.core.model.vo.AiModelConfigVO;
import com.coding.core.service.AiModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型服务实现
 */
@Service
@RequiredArgsConstructor
public class AiModelConfigServiceImpl implements AiModelConfigService {

    private final AiServiceConfigProperties configProperties;

    @Override
    public List<AiModelConfigVO> getEnabledModelsByType(String modelType) {
        return configProperties.getModels().stream()
                .filter(config -> config.getModelType().equals(modelType))
                .filter(config -> Boolean.TRUE.equals(config.getStatus()))
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private AiModelConfigVO convertToVO(AiServiceConfigProperties.ModelConfig config) {
        return AiModelConfigVO.builder()
                .provider(config.getProviderCode())
                .providerName(config.getProviderName())
                .modelType(config.getModelType())
                .modelId(config.getModelId())
                .build();
    }
}
