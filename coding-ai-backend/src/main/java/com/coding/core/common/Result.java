package com.coding.core.common;

import com.coding.workflow.enums.ErrorCodeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装类
 *
 * @param <T> 数据类型
 */
@Data
@Schema(description = "统一响应结果")
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    @Schema(description = "响应码")
    private Integer code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(1, "操作成功", null);
    }

    /**
     * 成功响应带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(1, "操作成功", data);
    }

    /**
     * 成功响应带数据和消息
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(1, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(0, "操作失败", null);
    }

    /**
     * 失败响应带消息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(0, message, null);
    }

    /**
     * 失败响应带错误码和消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 根据错误码枚举创建失败响应
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode) {
        return new Result<>(errorCode.getStatusCode(), errorCode.getMessage(), null);
    }

    /**
     * 根据错误码枚举和数据创建失败响应
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode, T data) {
        return new Result<>(errorCode.getStatusCode(), errorCode.getMessage(), data);
    }

    /**
     * 判断是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code != null && this.code == 1;
    }

    /**
     * 判断是否失败
     */
    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }
}
