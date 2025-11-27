package com.coding.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用信息 VO（通用）
 * @author coding
 * @date 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工具调用信息")
public class AgentToolCallVO {
    
    @Schema(description = "工具调用ID")
    private String id;
    
    @Schema(description = "工具名称")
    private String name;
    
    @Schema(description = "工具参数（JSON字符串）")
    private String arguments;
}

