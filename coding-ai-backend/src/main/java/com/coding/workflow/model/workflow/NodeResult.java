package com.coding.workflow.model.workflow;

import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.model.chat.ModelUsage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流节点执行结果
 */
@Data
public class NodeResult {
    /** 节点Id */
    @JsonProperty("node_id")
    private String nodeId;

    /** 节点名称 */
    @JsonProperty("node_name")
    private String nodeName;

    /** 节点类型 */
    @JsonProperty("node_type")
    private String nodeType;

    /** 节点状态 */
    @JsonProperty("node_status")
    private String nodeStatus;

    /** 节点执行开始时间 */
    @JsonProperty("node_start_time")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nodeStartTime;

    /** 节点执行时间 */
    @JsonProperty("node_execute_time")
    private String nodeExecuteTime;

    /** 错误码 */
    @JsonProperty("error_code")
    private String errorCode;

    /** 错误信息 */
    @JsonProperty("error_info")
    private String errorInfo;

    /** 输入结果：JSON */
    private String input;

    /** 输出结果：JSON */
    private String output;

    /** 输出类型 */
    @JsonProperty("output_type")
    private String outputType = "json";

    /** 模型Token用量 */
    private List<ModelUsage> usages;

    /** 是否多分支节点：用于条件边的判断 */
    @JsonProperty("is_multi_branch")
    private boolean isMultiBranch = false;

    /** 多分支节点结果：用于跳过某些条件边 */
    @JsonProperty("multi_branch_results")
    private List<MultiBranchReference> multiBranchResults;

    public static NodeResult error(Node node, String errorMsg) {
        NodeResult result = new NodeResult();
        result.setNodeId(node.getId());
        result.setNodeName(node.getName() == null ? node.getId() : node.getName());
        result.setNodeType(node.getType());
        result.setNodeStatus(NodeStatusEnum.FAIL.getCode());
        result.setErrorInfo(errorMsg);
        return result;
    }

    @Data
    public static class MultiBranchReference implements Serializable {

        @Serial
        private static final long serialVersionUID = -1L;

        /** 条件Id */
        @JsonProperty("condition_id")
        private String conditionId;

        /** 目标节点Id */
        @JsonProperty("target_ids")
        private List<String> targetIds;

    }
}
