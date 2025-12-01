package com.coding.agentflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.agentflow.model.request.AgentFlowConfigRequest;
import com.coding.agentflow.model.response.AgentFlowConfigResponse;
import com.coding.agentflow.service.AgentFlowConfigService;
import com.coding.core.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/agent-flow")
@AllArgsConstructor
@Tag(name = "Agent工作流配置管理")
public class AgentFlowController {

    private final AgentFlowConfigService agentFlowConfigService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<AgentFlowConfigResponse>> page(@RequestParam(defaultValue = "1") Integer current,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       @RequestParam(required = false) String name) {
        return Result.success(agentFlowConfigService.pageAgentFlows(current, size, name));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询")
    public Result<AgentFlowConfigResponse> getById(@PathVariable Long id) {
        return Result.success(agentFlowConfigService.getAgentFlowById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "保存或更新")
    public Result<Boolean> save(@RequestBody AgentFlowConfigRequest request) {
        return Result.success(agentFlowConfigService.saveOrUpdateAgentFlow(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(agentFlowConfigService.removeAgentFlow(id));
    }
}
