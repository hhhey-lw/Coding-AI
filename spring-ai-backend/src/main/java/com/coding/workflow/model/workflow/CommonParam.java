package com.coding.workflow.model.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用参数类
 */
@Data
public class CommonParam implements Serializable {
    // 参数名称
    private String key;

    // 参数类型
    private String type;

    // 参数描述
    private String desc;

    // 参数值
    private Object value;

    // 值的来源，例如推断和输入
    @JsonProperty("value_from")
    private String valueFrom;

    // 是否必需，true表示必需，false表示不必需
    private Boolean required;

    // 默认值
    @JsonProperty("default_value")
    private Object defaultValue;
}
