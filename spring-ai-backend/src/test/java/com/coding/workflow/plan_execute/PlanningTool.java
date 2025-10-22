package com.coding.workflow.plan_execute;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * PlanningTool 是一个用于创建、管理和跟踪复杂任务计划的工具。
 * 支持用户通过结构化的指令创建计划、更新步骤、标记进度、列出计划等，
 * 旨在帮助 AI Agent 更好地分解和执行多步骤任务。
 */
@Slf4j
public class PlanningTool implements BiFunction<PlanningTool.PlanningToolRequest, ToolContext, PlanningTool.PlanningToolResponse> {

	public static final String NAME = "planning";
	public static final String DESCRIPTION = "A tool for creating and managing multi-step plans to solve complex tasks.";

	// Singleton instance
	public static final PlanningTool INSTANCE = new PlanningTool();

	// ======================
	// 枚举：步骤状态
	// ======================
	/**
	 * 表示计划中某个步骤的当前执行状态。
	 */
	public enum StepStatus {
		NOT_STARTED,  // 未开始
		IN_PROGRESS,  // 进行中
		COMPLETED,    // 已完成
		BLOCKED       // 阻塞/受阻
	}

	// ======================
	// 请求类：结构化工具输入
	// ======================
	/**
	 * 封装 PlanningTool 的工具调用请求参数。
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化时忽略 null 值字段
	@JsonClassDescription("PlanningTool 的工具调用请求参数，用于创建、更新、管理任务计划")
	public static class PlanningToolRequest {

		@JsonProperty(value = "command", required = true) // 指定 JSON 字段名，标注必填（部分 JSON 库支持）
		@JsonPropertyDescription("操作命令，如：create, update, mark_step, list, get, set_active, delete。该字段为必填。")
		private String command;

		@JsonPropertyDescription("计划唯一标识符。如果未提供，将自动生成。")
		private String planId;

		@JsonPropertyDescription("计划标题，创建计划时必填，用于描述计划的目标或主题。")
		private String title;

		@JsonPropertyDescription("计划步骤列表，每个元素为字符串，表示一个执行步骤。创建计划时必填。")
		private List<String> steps;

		@JsonPropertyDescription("要更新的步骤索引（从0开始）。仅在 mark_step 命令中有效。")
		private Integer stepIndex;

		@JsonPropertyDescription("步骤的当前状态，如：NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED。仅在 mark_step 命令中有效。")
		private StepStatus stepStatus;

		@JsonPropertyDescription("步骤的附加备注信息，用于补充说明。为可选字段。")
		private String stepNotes;
	}

	// ======================
	// 响应类：结构化工具输出
	// ======================
	/**
	 * PlanningTool 工具的标准响应结构。
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL) // 忽略 null 字段，使输出更干净
	@JsonClassDescription("PlanningTool 工具调用的标准响应结构，包含输出内容和关联的计划ID")
	public static class PlanningToolResponse {

		@JsonPropertyDescription("工具执行结果的文本描述，通常为给用户的反馈信息。")
		private String output;

		@JsonPropertyDescription("与该响应关联的计划ID。如果操作涉及某个具体计划，则返回该计划的唯一标识符。")
		private String planId;

		@Override
		public String toString() {
			return output;
		}
	}

	// ======================
	// 工具内部状态
	// ======================
	private final Map<String, Map<String, Object>> plans = new HashMap<>();     // 所有计划元数据
	private final Map<String, Plan> graphPlans = new HashMap<>();              // 所有计划执行模型
	private String currentPlanId;                                              // 当前活跃计划

	// ======================
	// 工具注册与定义（Spring AI）
	// ======================
	@Override
	public PlanningToolResponse apply(PlanningToolRequest toolRequest, ToolContext context) {
		return run(toolRequest, context);
	}

	public PlanningToolResponse run(PlanningToolRequest request, ToolContext context) {
		try {
			String command = request.getCommand();
			String planId = request.getPlanId();
			if (planId == null || planId.trim().isEmpty()) {
				planId = "G_" + UUID.randomUUID();
			}

			switch (command != null ? command.toLowerCase() : "") {
				case "create":
					String title = request.getTitle();
					List<String> steps = request.getSteps();
					return createPlan(planId, title, steps, context);

				default:
					throw new IllegalArgumentException("Unsupported command: " + command +
							". Supported commands include: create");
			}
		} catch (Exception e) {
			throw new RuntimeException("Planning tool execution failed: " + e.getMessage(), e);
		}
	}

	// ======================
	// 创建计划
	// ======================
	public PlanningToolResponse createPlan(String planId, String title, List<String> steps, ToolContext context) {
		if (planId == null || planId.trim().isEmpty()) {
			throw new IllegalArgumentException("plan_id is required.");
		}
		if (plans.containsKey(planId)) {
			throw new IllegalArgumentException("Plan with ID '" + planId + "' already exists.");
		}
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("title is required.");
		}
		if (steps == null || steps.isEmpty()) {
			throw new IllegalArgumentException("steps list cannot be empty.");
		}

		// 构造元数据
		Map<String, Object> meta = new HashMap<>();
		meta.put("plan_id", planId);
		meta.put("title", title);
		meta.put("steps", steps);
		meta.put("step_statuses", steps.stream().map(s -> StepStatus.NOT_STARTED).collect(Collectors.toList()));
		meta.put("step_notes", steps.stream().map(s -> "").collect(Collectors.toList()));

		plans.put(planId, meta);

		// 构造执行模型
		Plan planModel = new Plan(title, planId, steps);
		graphPlans.put(planId, planModel);

		this.currentPlanId = planId;

		// 构造完整的计划信息JSON
		Map<String, Object> planOutput = new HashMap<>();
		planOutput.put("planId", planId);
		planOutput.put("title", title);
		planOutput.put("steps", steps);

		String planJson = cn.hutool.json.JSONUtil.toJsonStr(planOutput);
		return new PlanningToolResponse(planJson, planId);
	}

	public Plan getGraphPlan(String planId) {
		if (planId == null || planId.isEmpty()) {
			if (currentPlanId == null) {
				throw new RuntimeException("No active plan. Please specify a plan_id or set an active plan.");
			}
			planId = currentPlanId;
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		return graphPlans.get(planId);
	}

	// ======================
	// 内部类：Plan（用于执行与状态跟踪）
	// ======================
	@Data
	public class Plan {
		private Map<String, String> stepStatus;
		private int currentStep = 0;
		private String task;
		private String planId;
		private List<String> steps;

		public Plan(String task, String planId, List<String> steps) {
			this.task = task;
			this.planId = planId;
			this.steps = steps;
			this.stepStatus = new HashMap<>();
		}

		public String getCurrentStep() {
			return String.valueOf(currentStep);
		}

		public void updateStepStatus(String stepIndex, String status) {
			stepStatus.put(stepIndex, status);
		}

		public String nextStepPrompt() {
			String nextStepDescription = steps.get(currentStep);
			Map<String, Object> context = new HashMap<>();
			context.put("task", task);
			context.put("planWithSteps", steps);
			context.put("stepIndex", currentStep);
			context.put("nextStepDescription", nextStepDescription);
			context.put("stepStatus", stepStatus);

			currentStep++;

			String template = """
				The task is: {task}

				You are asked to follow the following plan with specific sequential steps to complete this task:
				{planWithSteps}

				You are currently at step {stepIndex} of the plan, which is: {nextStepDescription}.

				Below are the result of the previous steps, which you can use as the context to help you complete the current step:
				  {stepStatus}

				""";
			PromptTemplate promptTemplate = new PromptTemplate(template);
			return promptTemplate.render(context);
		}

		public String nextStep() {
			return steps.get(currentStep++);
		}

		public boolean isFinished() {
			return currentStep == steps.size();
		}

		void setSteps(List<String> steps) {
			this.steps = steps;
		}
	}

	// ======================
	// 工具注册方法（供外部调用）
	// ======================
	public static FunctionTool getToolDefinition() {
		FunctionTool.Function function = new FunctionTool.Function(DESCRIPTION, NAME, getParametersJson());
		return new FunctionTool(function);
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(NAME, INSTANCE)
				.description(DESCRIPTION)
				.inputType(PlanningToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
				.build();
	}

	private static String getParametersJson() {
		return """
            {
                "type": "object",
                "properties": {
                    "command": { "type": "string", "description": "Command: create" },
                    "plan_id": { "type": "string", "description": "Plan ID" },
                    "title": { "type": "string", "description": "Plan title" },
                    "steps": { "type": "array", "items": { "type": "string" }, "description": "List of steps" },
                    "step_index": { "type": "integer", "description": "Step index" },
                    "step_status": { "type": "string", "description": "Step status" },
                    "step_notes": { "type": "string", "description": "Step notes" }
                },
                "required": ["command"]
            }
            """;
	}

	// ======================
	// Getter（调试用）
	// ======================
	public Map<String, Map<String, Object>> getPlans() {
		return plans;
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}
}