package com.coding.agentflow.model.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "二维坐标位置")
public class Position {

    @Schema(name = "X坐标")
    private Double x;

    @Schema(name = "Y坐标")
    private Double y;

}