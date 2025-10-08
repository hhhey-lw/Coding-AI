package com.coding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.admin.mapper.McpServerMapper;
import com.coding.admin.model.entity.McpServerDO;
import com.coding.admin.model.vo.McpServerVO;
import com.coding.admin.service.McpServerService;
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
public class McpServerServiceImpl implements McpServerService {

    private final McpServerMapper mcpServerMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<McpServerVO> getEnabledMcpServers() {
        LambdaQueryWrapper<McpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServerDO::getStatus, Boolean.TRUE);
        List<McpServerDO> mcpServers = mcpServerMapper.selectList(wrapper);

        return mcpServers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public McpServerDO getByServerCode(String serverCode) {
        LambdaQueryWrapper<McpServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServerDO::getServerCode, serverCode)
               .eq(McpServerDO::getStatus, Boolean.TRUE);
        return mcpServerMapper.selectOne(wrapper);
    }

    private McpServerVO convertToVO(McpServerDO mcpServerDO) {
        McpServerVO vo = new McpServerVO();
        vo.setServerName(mcpServerDO.getServerName());
        vo.setServerCode(mcpServerDO.getServerCode());
        vo.setToolName(mcpServerDO.getToolName());
        vo.setContentDescription(mcpServerDO.getContentDescription());

        // 解析JSON格式的工具参数
        try {
            if (mcpServerDO.getToolParams() != null) {
                List<McpServerVO.ToolParam> toolParams = objectMapper.readValue(
                    mcpServerDO.getToolParams(),
                    new TypeReference<List<McpServerVO.ToolParam>>() {}
                );
                vo.setToolParams(toolParams);
            }
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", mcpServerDO.getToolParams(), e);
        }

        return vo;
    }
}
