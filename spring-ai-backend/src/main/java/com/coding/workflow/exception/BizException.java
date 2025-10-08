package com.coding.workflow.exception;

import com.coding.workflow.exception.error.Error;
import lombok.*;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class BizException extends RuntimeException implements Serializable {
    private com.coding.workflow.exception.error.Error error;

    // 构造函数
    public BizException() {
        super();
    }

    public BizException(com.coding.workflow.exception.error.Error error) {
        super(error.getMessage());
        this.error = error;
    }

    public BizException(com.coding.workflow.exception.error.Error error, String message) {
        super(message);
        this.error = error;
        this.error.setMessage(message);
    }

    public BizException(String message) {
        super(message);
        this.error = Error.builder().message(message).build();
    }
}
