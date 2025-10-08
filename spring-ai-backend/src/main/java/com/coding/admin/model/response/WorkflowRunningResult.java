package com.coding.admin.model.response;

import com.coding.admin.model.vo.WorkflowInstanceVO;
import com.coding.admin.model.vo.WorkflowNodeInstanceVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作流运行结果")
public class WorkflowRunningResult {
    @Schema(description = "工作流实例")
    private WorkflowInstanceVO workflowInstanceVO;
    @Schema(description = "工作流节点实例列表")
    private List<WorkflowNodeInstanceVO> workflowNodeInstanceVOList;
}
