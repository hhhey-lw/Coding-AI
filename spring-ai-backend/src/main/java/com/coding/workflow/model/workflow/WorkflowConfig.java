package com.coding.workflow.model.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流配置类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConfig implements Serializable {
    private List<Node> nodes;
    private List<Edge> edges;

    @JsonProperty("global_config")
    private GlobalConfig globalConfig;

    @Data
    public static class GlobalConfig implements Serializable {

        @JsonProperty("variable_config")
        private VariableConfig variableConfig;

    }

    @Data
    public static class VariableConfig implements Serializable {

        @JsonProperty("conversation_params")
        private List<CommonParam> conversationParams;

    }

}
