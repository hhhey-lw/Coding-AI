package com.coding.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.core.mapper.AiModelConfigMapper;
import com.coding.core.model.entity.AiModelConfigDO;
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

    private final AiModelConfigMapper aiModelConfigMapper;

    @Override
    public List<AiModelConfigVO> getEnabledModelsByType(String modelType) {
        LambdaQueryWrapper<AiModelConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfigDO::getModelType, modelType)
               .eq(AiModelConfigDO::getStatus, 1);
        List<AiModelConfigDO> aiModels = aiModelConfigMapper.selectList(wrapper);

        return aiModels.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private AiModelConfigVO convertToVO(AiModelConfigDO aiModelConfigDO) {
        return AiModelConfigVO.builder()
                .provider(aiModelConfigDO.getProviderCode())
                .providerName(aiModelConfigDO.getProviderName())
                .modelType(aiModelConfigDO.getModelType())
                .modelId(aiModelConfigDO.getModelId())
                .build();
    }
}
