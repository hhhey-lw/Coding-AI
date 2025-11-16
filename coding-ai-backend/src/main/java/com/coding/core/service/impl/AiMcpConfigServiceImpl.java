package com.coding.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.core.mapper.AiMcpConfigMapper;
import com.coding.core.model.entity.AiMcpConfigDO;
import com.coding.core.model.vo.AiMcpConfigVO;
import com.coding.core.service.AiMcpConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP服务器服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMcpConfigServiceImpl implements AiMcpConfigService {

    private final AiMcpConfigMapper aiMcpConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiMcpConfigVO> getEnabledMcpServers() {
        LambdaQueryWrapper<AiMcpConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpConfigDO::getStatus, Boolean.TRUE);
        List<AiMcpConfigDO> mcpServers = aiMcpConfigMapper.selectList(wrapper);

        return mcpServers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AiMcpConfigDO getByServerCode(String serverCode) {
        LambdaQueryWrapper<AiMcpConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpConfigDO::getServerCode, serverCode)
               .eq(AiMcpConfigDO::getStatus, Boolean.TRUE);
        return aiMcpConfigMapper.selectOne(wrapper);
    }

    private AiMcpConfigVO convertToVO(AiMcpConfigDO aiMcpConfigDO) {
        AiMcpConfigVO vo = new AiMcpConfigVO();
        vo.setServerName(aiMcpConfigDO.getServerName());
        vo.setServerCode(aiMcpConfigDO.getServerCode());
        vo.setToolName(aiMcpConfigDO.getToolName());
        vo.setContentDescription(aiMcpConfigDO.getContentDescription());

        // 解析JSON格式的工具参数
        try {
            if (aiMcpConfigDO.getToolParams() != null) {
                List<AiMcpConfigVO.ToolParam> toolParams = objectMapper.readValue(
                    aiMcpConfigDO.getToolParams(),
                    new TypeReference<List<AiMcpConfigVO.ToolParam>>() {}
                );
                vo.setToolParams(toolParams);
            }
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", aiMcpConfigDO.getToolParams(), e);
        }

        return vo;
    }
}
