package com.coding.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coding.admin.model.converter.WorkflowConfigConverter;
import com.coding.admin.model.model.WorkflowConfigModel;
import com.coding.admin.model.request.WorkflowConfigAddRequest;
import com.coding.admin.model.request.WorkflowConfigUpdateRequest;
import com.coding.admin.model.request.WorkflowConfigUpdateStatusRequest;
import com.coding.admin.model.response.WorkflowRunningResult;
import com.coding.admin.model.vo.PageVO;
import com.coding.admin.model.vo.WorkflowConfigVO;
import com.coding.admin.service.WorkflowConfigService;
import com.coding.admin.common.Result;
import com.coding.workflow.utils.AssertUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/workflow")
@Tag(name = "工作流管理", description = "工作流配置的增删改查及运行接口")
public class WorkflowController {
    @Resource
    private WorkflowConfigService workflowConfigService;

    @PostMapping("/add")
    @Operation(summary = "新增工作流配置", description = "创建新的工作流配置")
    public Result<Long> addWorkflowConfig(@RequestBody @Validated WorkflowConfigAddRequest request) {
        WorkflowConfigModel model = new WorkflowConfigModel();
        BeanUtils.copyProperties(request, model);
        Long id = workflowConfigService.addWorkflowConfig(model);
        return Result.success("工作流配置创建成功", id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作流信息", description = "更新已存在的工作流配置")
    public Result<Boolean> updateWorkflowConfig(@RequestBody @Validated WorkflowConfigUpdateRequest request) {
        WorkflowConfigModel model = new WorkflowConfigModel();
        BeanUtils.copyProperties(request, model);
        boolean result = workflowConfigService.updateWorkflowConfig(model);
        return result ? Result.success("工作流配置更新成功", true) : Result.error("工作流配置更新失败");
    }

    @PutMapping("/updateStatus")
    @Operation(summary = "更新工作流状态", description = "启用或禁用工作流配置")
    public Result<Boolean> updateWorkflowStatus(@RequestBody @Validated WorkflowConfigUpdateStatusRequest request) {
        WorkflowConfigModel model = workflowConfigService.getWorkflowConfigById(request.getId());
        if (model == null) {
            return Result.error("工作流配置不存在");
        }
        model.setStatus(request.getStatus());
        boolean result = workflowConfigService.updateWorkflowConfig(model);
        return result ? Result.success("工作流状态更新成功", true) : Result.error("工作流状态更新失败");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询工作流信息", description = "根据ID获取工作流配置详情")
    public Result<WorkflowConfigVO> getWorkflowConfig(
            @Parameter(description = "工作流配置ID") @PathVariable String id) {
        WorkflowConfigModel model = workflowConfigService.getWorkflowConfigById(id);
        if (model == null) {
            return Result.error("工作流配置不存在");
        }
        return Result.success("获取成功", WorkflowConfigConverter.INSTANCE.modelToVO(model));
    }

    @GetMapping("/my/list")
    @Operation(summary = "查询我的工作流列表（分页）", description = "分页查询当前登录用户创建的工作流")
    public Result<PageVO<WorkflowConfigVO>> getMyWorkflowPage(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "工作流名称（模糊查询）") @RequestParam(required = false) String workflowName) {

        Page<WorkflowConfigModel> page = workflowConfigService.getMyWorkflowPage(workflowName, pageNum, pageSize);

        // 转换为VO
        List<WorkflowConfigVO> voList = page.getRecords().stream()
                .map(WorkflowConfigConverter.INSTANCE::modelToVO)
                .collect(java.util.stream.Collectors.toList());

        // 构建分页响应
        PageVO<WorkflowConfigVO> pageVO = PageVO.of(
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getTotal(),
                voList
        );

        return Result.success("查询成功", pageVO);
    }

    @PostMapping("/run/{workflowId}")
    @Operation(summary = "运行工作流", description = "执行指定的工作流")
    public Result<Long> runWorkflow(
            @Parameter(description = "工作流ID") @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, Object> inputParams) {
        Long instanceId = workflowConfigService.runWorkflow(workflowId, inputParams);
        return Result.success("工作流启动成功", instanceId);
    }

    @GetMapping("/result/{workflowInstanceId}")
    @Operation(summary = "查询运行结果", description = "根据工作流实例ID获取运行结果")
    public Result<WorkflowRunningResult> getWorkflowRunningResult(
            @Parameter(description = "工作流实例ID") @PathVariable String workflowInstanceId) {
        AssertUtil.isNotNull(workflowInstanceId, "工作流实例ID不能为空");
        return Result.success(workflowConfigService.getWorkflowRunningResult(workflowInstanceId));
    }

}
