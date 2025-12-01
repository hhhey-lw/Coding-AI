package com.coding.agentflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coding.agentflow.mapper.AgentFlowConfigMapper;
import com.coding.agentflow.model.entity.AgentFlowConfig;
import com.coding.agentflow.model.request.AgentFlowConfigRequest;
import com.coding.agentflow.model.response.AgentFlowConfigResponse;
import com.coding.agentflow.repository.AgentFlowConfigRepository;
import com.coding.agentflow.service.AgentFlowConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Agent流程配置 服务实现类
 */
@Service
@AllArgsConstructor
public class AgentFlowConfigServiceImpl extends ServiceImpl<AgentFlowConfigMapper, AgentFlowConfig> implements AgentFlowConfigService {

    private final AgentFlowConfigRepository agentFlowConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<AgentFlowConfigResponse> pageAgentFlows(Integer current, Integer size, String name) {
        Page<AgentFlowConfig> page = agentFlowConfigRepository.pageAgentFlows(current, size, name);
        return convertToResponsePage(page);
    }

    @Override
    public AgentFlowConfigResponse getAgentFlowById(Long id) {
        AgentFlowConfig config = this.getById(id);
        return convertToResponse(config);
    }

    @Override
    public Boolean saveOrUpdateAgentFlow(AgentFlowConfigRequest request) {
        AgentFlowConfig config = convertToEntity(request);
        return this.saveOrUpdate(config);
    }

    @Override
    public Boolean removeAgentFlow(Long id) {
        return this.removeById(id);
    }

    /**
     * 转换分页结果
     */
    private Page<AgentFlowConfigResponse> convertToResponsePage(Page<AgentFlowConfig> page) {
        Page<AgentFlowConfigResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream()
                .map(this::convertToResponse)
                .toList());
        return responsePage;
    }

    /**
     * DO 转 Response
     */
    private AgentFlowConfigResponse convertToResponse(AgentFlowConfig config) {
        if (config == null) {
            return null;
        }
        AgentFlowConfigResponse response = new AgentFlowConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setDescription(config.getDescription());
        response.setStatus(config.getStatus());
        response.setCreatorId(config.getCreatorId());
        response.setCreateTime(config.getCreateTime());
        response.setUpdateTime(config.getUpdateTime());

        // 将 JSON 字符串转换为对象列表
        try {
            if (config.getNodes() != null) {
                response.setNodes(objectMapper.readValue(config.getNodes(), new TypeReference<>() {}));
            }
            if (config.getEdges() != null) {
                response.setEdges(objectMapper.readValue(config.getEdges(), new TypeReference<>() {}));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
        return response;
    }

    /**
     * Request 转 DO
     */
    private AgentFlowConfig convertToEntity(AgentFlowConfigRequest request) {
        AgentFlowConfig config = new AgentFlowConfig();
        config.setId(request.getId());
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setStatus(request.getStatus());

        // 将对象列表转换为 JSON 字符串
        try {
            if (request.getNodes() != null) {
                config.setNodes(objectMapper.writeValueAsString(request.getNodes()));
            }
            if (request.getEdges() != null) {
                config.setEdges(objectMapper.writeValueAsString(request.getEdges()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
        return config;
    }
}
