package com.coding.admin.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页请求基类
 */
@Data
@Schema(name = "分页请求")
public class PageRequest {

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    private Integer pageSize = 10;

    /**
     * 获取MyBatis-Plus的offset
     */
    @JsonIgnore
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

    /**
     * 获取limit
     */
    @JsonIgnore
    public long getLimit() {
        return pageSize;
    }
}
