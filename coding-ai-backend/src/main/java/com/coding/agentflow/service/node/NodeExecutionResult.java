package com.coding.agentflow.service.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 节点执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 执行结果数据
     */
    private Map<String, Object> data;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;

    /**
     * 创建成功结果
     */
    public static NodeExecutionResult success(Map<String, Object> data) {
        return NodeExecutionResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NodeExecutionResult failure(String errorMessage) {
        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

}
