package com.coding.core.service.impl;

import com.coding.core.config.AiServiceConfigProperties;
import com.coding.core.model.vo.AiMcpConfigVO;
import com.coding.core.service.AiMcpConfigService;
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

    private final AiServiceConfigProperties configProperties;

    @Override
    public List<AiMcpConfigVO> getEnabledMcpServers() {
        return configProperties.getMcpServers().stream()
                .filter(config -> Boolean.TRUE.equals(config.getStatus()))
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AiServiceConfigProperties.McpServerConfig getByServerCode(String serverCode) {
        return configProperties.getMcpServers().stream()
                .filter(config -> config.getServerCode().equals(serverCode))
                .filter(config -> Boolean.TRUE.equals(config.getStatus()))
                .findFirst()
                .orElse(null);
    }

    private AiMcpConfigVO convertToVO(AiServiceConfigProperties.McpServerConfig config) {
        AiMcpConfigVO vo = new AiMcpConfigVO();
        vo.setServerName(config.getServerName());
        vo.setServerCode(config.getServerCode());
        vo.setToolName(config.getToolName());
        vo.setContentDescription(config.getContentDescription());

        // 转换工具参数
        if (config.getToolParams() != null) {
            List<AiMcpConfigVO.ToolParam> toolParams = config.getToolParams().stream()
                    .map(param -> AiMcpConfigVO.ToolParam.of(
                            param.getKey(),
                            param.getType(),
                            param.getDesc()
                    ))
                    .collect(Collectors.toList());
            vo.setToolParams(toolParams);
        }

        return vo;
    }
}
