package com.coding.workflow.model.workflow;

import com.coding.workflow.enums.ValueFromEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流节点模型
 */
@Data
public class Node implements Serializable {
    /** 节点id */
    private String id;
    /** 节点名称 */
    private String name;
    /** 节点描述 */
    private String desc;
    /** 节点类型 */
    private String type;
    /** 节点配置 */
    private NodeCustomConfig config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeCustomConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = -1L;

        /** 入参配置 */
        @JsonProperty("input_params")
        private List<InputParam> inputParams;

        /** 出参配置 */
        @JsonProperty("output_params")
        private List<OutputParam> outputParams;

        /** 节点配置 */
        @JsonProperty("node_param")
        private Map<String, Object> nodeParam;

        public static NodeCustomConfig of(List<InputParam> inputParams, List<OutputParam> outputParams, Map<String, Object> nodeParam) {
            return new NodeCustomConfig(inputParams, outputParams, nodeParam);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InputParam extends CommonParam implements Serializable {

        @Serial
        private static final long serialVersionUID = -1L;

        public static InputParam of(String key, String type, ValueFromEnum valueFrom, String value) {
            InputParam inputParam = new InputParam();
            inputParam.setKey(key);
            inputParam.setType(type);
            inputParam.setValueFrom(valueFrom.name());
            inputParam.setValue(value);
            return inputParam;
        }

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OutputParam extends CommonParam implements Serializable {

        @Serial
        private static final long serialVersionUID = -1L;

        private List<OutputParam> properties;

    }
}
