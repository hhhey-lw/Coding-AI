package com.coding.agentflow.model.model;

import com.coding.agentflow.model.enums.ParamTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(name = "节点输入参数定义")
public class CommonParam {

    @Schema(name = "参数标签")
    private String label;

    @Schema(name = "参数名称")
    private String name;

    @Schema(name = "参数类型")
    private ParamTypeEnum type;

    @Schema(name = "参数值")
    private Object value;

    @Schema(name = "默认值")
    private Object defaultValue;

    @Schema(name = "是否必须", example = "true")
    private Boolean required;

    @Schema(name = "是否为变量")
    private Boolean isVariable;

}