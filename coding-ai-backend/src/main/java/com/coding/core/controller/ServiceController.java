package com.coding.core.controller;

import com.coding.core.common.Result;
import com.coding.core.enums.ModelTypeEnum;
import com.coding.core.model.vo.AiModelConfigVO;
import com.coding.core.model.vo.AiMcpConfigVO;
import com.coding.core.service.AiModelConfigService;
import com.coding.core.service.AiMcpConfigService;
import com.coding.workflow.utils.AssertUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务管理控制器
 * TODO 优化为字典表查询
 */
@Tag(name = "服务管理")
@RestController
@RequiredArgsConstructor
public class ServiceController {

    private final AiMcpConfigService aiMcpConfigService;
    private final AiModelConfigService aiModelConfigService;

    @Operation(summary = "获取MCP服务列表")
    @GetMapping("/mcp/list")
    public Result<List<AiMcpConfigVO>> getMcpServerList() {
        List<AiMcpConfigVO> mcpServers = aiMcpConfigService.getEnabledMcpServers();
        return Result.success(mcpServers);
    }

    @Operation(summary = "获取AI模型列表")
    @GetMapping("/model/list")
    public Result<List<AiModelConfigVO>> getModelList(@RequestParam("modelType") String modelType) {
        AssertUtil.isNotBlank(modelType, "模型类型不能为空");
        AssertUtil.isTrue(ModelTypeEnum.isValidEnum(modelType), "模型类型不合法");

        List<AiModelConfigVO> models = aiModelConfigService.getEnabledModelsByType(modelType);
        return Result.success(models);
    }
}
