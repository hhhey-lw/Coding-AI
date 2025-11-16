package com.coding.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {
    INVALID_PARAMS(0, "invalid_request_error", "InvalidParameter", "参数 %s 是无效的, %s."),
    WORKFLOW_EXECUTE_ERROR(1, "response_error", "WorkflowExecuteError", "工作流执行失败, 错误源： %s"),
    WORKFLOW_CONFIG_ERROR(2, "invalid_request_error", "WorkflowConfigError", "工作流配置错误, 错误源： %s"),
    ;
    /**
     * http status code.
     */
    private final int statusCode;

    private final String type;

    /**
     * error code
     */
    private final String code;

    /**
     * error message.
     */
    private final String message;


}
