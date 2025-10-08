package com.coding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.admin.mapper.AiModelMapper;
import com.coding.admin.model.entity.AiModelDO;
import com.coding.admin.model.vo.AiModelBaseVO;
import com.coding.admin.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型服务实现
 */
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelMapper aiModelMapper;

    @Override
    public List<AiModelBaseVO> getEnabledModelsByType(String modelType) {
        LambdaQueryWrapper<AiModelDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelDO::getModelType, modelType)
               .eq(AiModelDO::getStatus, 1);
        List<AiModelDO> aiModels = aiModelMapper.selectList(wrapper);

        return aiModels.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private AiModelBaseVO convertToVO(AiModelDO aiModelDO) {
        return AiModelBaseVO.builder()
                .provider(aiModelDO.getProviderCode())
                .providerName(aiModelDO.getProviderName())
                .modelType(aiModelDO.getModelType())
                .modelId(aiModelDO.getModelId())
                .build();
    }
}
