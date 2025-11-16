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
    /** 工作流节点列表 */
    private List<Node> nodes;
    /** 工作流边列表 */
    private List<Edge> edges;

    /** 工作流全局配置 */
    @JsonProperty("global_config")
    private GlobalConfig globalConfig;

    @Data
    public static class GlobalConfig implements Serializable {

        /** 全局变量列表 */
        @JsonProperty("variable_config")
        private VariableConfig variableConfig;

    }

    @Data
    public static class VariableConfig implements Serializable {

        /** 会话变量 */
        @JsonProperty("conversation_params")
        private List<CommonParam> conversationParams;

    }

}
