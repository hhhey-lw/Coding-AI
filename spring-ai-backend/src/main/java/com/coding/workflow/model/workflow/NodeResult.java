package com.coding.workflow.model.workflow;

import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.model.chat.ModelUsage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import com.coding.workflow.exception.error.Error;
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
    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("node_name")
    private String nodeName;

    @JsonProperty("node_type")
    private String nodeType;

    @JsonProperty("node_status")
    private String nodeStatus;

    @JsonProperty("node_start_time")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nodeStartTime;

    @JsonProperty("node_execute_time")
    private String nodeExecuteTime;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_info")
    private String errorInfo;

    private Error error;

    private String input;

    private String output;

    @JsonProperty("output_type")
    private String outputType = "json";

    private List<ModelUsage> usages;

    // 是否多分支节点：用于条件边的判断
    @JsonProperty("is_multi_branch")
    private boolean isMultiBranch = false;

    // 多分支节点结果：用于跳过某些条件边
    @JsonProperty("multi_branch_results")
    private List<MultiBranchReference> multiBranchResults;

    public static NodeResult error(Node node, Error error) {
        NodeResult result = new NodeResult();
        result.setNodeId(node.getId());
        result.setNodeName(node.getName() == null ? node.getId() : node.getName());
        result.setNodeType(node.getType());
        result.setNodeStatus(NodeStatusEnum.FAIL.getCode());
        result.setErrorInfo(error.getMessage());
        result.setError(error);
        return result;
    }

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

        @JsonProperty("condition_id")
        private String conditionId;

        @JsonProperty("target_ids")
        private List<String> targetIds;

    }
}
