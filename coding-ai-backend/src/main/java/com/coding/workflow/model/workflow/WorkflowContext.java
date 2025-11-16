package com.coding.workflow.model.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.coding.workflow.model.chat.ModelUsage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流上下文类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext {
    /** 应用信息 */
    private Long appId;
    private Long configId;
    private Long instanceId;
    private Long conversationId;
    /** 任务状态 */
    private String taskStatus;
    private String taskResult;
    private String errorCode;
    private String errorInfo;
    /** 参数和配置类 */
    private Map<String, Object> userMap = Maps.newHashMap();
    private Map<String, Object> sysMap = Maps.newHashMap();
    private WorkflowConfig workflowConfig;
    /** 变量和结果Map */
    private Map<String, Object> variablesMap = Maps.newHashMap();
    private Map<String, NodeResult> nodeResultMap = Maps.newHashMap();
    /** 用量和模型Key */
    private List<ModelUsage> modelUsages;
    /** 统计信息 */
    private LocalDateTime startTime;
    /** 确保每个时刻仅一个节点执行 */
    @JsonIgnore
    private transient Lock lock = new ReentrantLock();
}
