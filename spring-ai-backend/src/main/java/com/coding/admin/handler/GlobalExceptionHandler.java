package com.coding.admin.handler;

import com.coding.admin.common.Result;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return Result.error("上传文件大小超过限制，最大支持10MB");
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 忽略 SSE 流式连接的客户端断开异常（这是正常现象）
        if (e instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException) {
            log.debug("客户端断开 SSE 连接（正常）: {}", e.getMessage());
            return null;  // 不返回任何内容
        }
        
        // 忽略客户端主动取消连接的异常
        if (e.getCause() instanceof org.apache.catalina.connector.ClientAbortException) {
            log.debug("客户端主动断开连接（正常）: {}", e.getMessage());
            return null;
        }
        
        log.error("系统异常", e);
        return Result.error("系统异常，请稍后重试");
    }
}
