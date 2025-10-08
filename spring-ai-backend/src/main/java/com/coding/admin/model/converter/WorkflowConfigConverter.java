package com.coding.admin.model.converter;

import com.coding.admin.model.entity.WorkflowConfigDO;
import com.coding.admin.model.model.WorkflowConfigModel;
import com.coding.admin.model.vo.WorkflowConfigVO;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工作流配置转换器
 * @author coding
 * @date 2025-09-21
 */
@Mapper
public interface WorkflowConfigConverter {

    WorkflowConfigConverter INSTANCE = Mappers.getMapper(WorkflowConfigConverter.class);

    default List<Node> mapStringToListNode(String nodesJson) {
        if (nodesJson == null || nodesJson.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 假设 nodesJson 是一个 JSON 数组，比如："[{\"id\":\"1\",\"name\":\"start\"}]"
            return objectMapper.readValue(nodesJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse nodes JSON: " + nodesJson, e);
        }
    }

    default String mapListNodeToString(List<Node> nodes) {
        if (nodes == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize nodes to JSON: " + nodes, e);
        }
    }

    default List<Edge> mapStringToListEdge(String edgesJson) {
        if (edgesJson == null || edgesJson.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 假设 edgesJson 是一个 JSON 数组，例如："[{\"id\":\"1\",\"from\":\"A\",\"to\":\"B\"}]"
            return objectMapper.readValue(edgesJson, new TypeReference<List<Edge>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse edges JSON: " + edgesJson, e);
        }
    }

    // 手动实现：List<Edge> -> String（JSON）
    default String mapListEdgeToString(List<Edge> edges) {
        if (edges == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(edges);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize edges to JSON: " + edges, e);
        }
    }

    /**
     * DO转Model
     */
    WorkflowConfigModel doToModel(WorkflowConfigDO workflowConfigDO);

    /**
     * Model转VO
     */
    WorkflowConfigVO modelToVO(WorkflowConfigModel workflowConfigModel);

    /**
     * Model转DO
     */
    WorkflowConfigDO modelToDO(WorkflowConfigModel workflowConfigModel);
}
