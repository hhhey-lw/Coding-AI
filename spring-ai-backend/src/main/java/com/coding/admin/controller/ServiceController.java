package com.coding.admin.controller;

import com.coding.admin.common.Result;
import com.coding.admin.enums.ModelTypeEnum;
import com.coding.admin.model.vo.AiModelBaseVO;
import com.coding.admin.model.vo.McpServerVO;
import com.coding.admin.service.AiModelService;
import com.coding.admin.service.McpServerService;
import com.coding.workflow.utils.AssertUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "服务管理")
@RestController
@RequiredArgsConstructor
public class ServiceController {

    private final McpServerService mcpServerService;
    private final AiModelService aiModelService;

    @Operation(summary = "获取MCP服务列表")
    @GetMapping("/mcp/list")
    public Result<List<McpServerVO>> getMcpServerList() {
        List<McpServerVO> mcpServers = mcpServerService.getEnabledMcpServers();
        return Result.success(mcpServers);
    }

    @Operation(summary = "获取AI模型列表")
    @GetMapping("/model/list")
    public Result<List<AiModelBaseVO>> getModelList(@RequestParam("modelType") String modelType) {
        AssertUtil.isNotBlank(modelType, "模型类型不能为空");
        AssertUtil.isTrue(ModelTypeEnum.isValidEnum(modelType), "模型类型不合法");

        List<AiModelBaseVO> models = aiModelService.getEnabledModelsByType(modelType);
        return Result.success(models);
    }
}
