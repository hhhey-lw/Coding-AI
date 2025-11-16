package com.coding.graph.core.agent.plan;

import cn.hutool.json.JSONUtil;
import com.coding.graph.core.node.action.NodeAction;
import com.coding.graph.core.state.OverAllState;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SupervisorAgent 负责监督和管理规划工具（PlanningTool）生成的计划执行过程。
 *
 * 设计目标：通过与 PlanningTool 的集成，SupervisorAgent 能够动态跟踪计划的执行状态，
 */
public class SupervisorAgent implements NodeAction {

    private final PlanningTool planningTool;

    public SupervisorAgent(PlanningTool planningTool) {
        this.planningTool = planningTool;
    }

    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {
        PlanningTool.Plan plan = planningTool.getGraphPlan();

        Optional<Object> optionalOutput = t.value("step_output");

        if (optionalOutput.isPresent()) {
            String finalStepOutput = String.format("This is the final output of step %s:\n %s", plan.getCurrentStep(),
                    optionalOutput.get());
            plan.updateStepStatus(plan.getCurrentStep(), finalStepOutput);
        }

        String promptForNextStep;
        if (!plan.isFinished()) {
            promptForNextStep = plan.nextStepPrompt((Map<String, String>) t.value("step_status_history").orElse(Map.of()));
        }
        else {
            promptForNextStep = "Plan completed.";
        }

        // 构造包含完整进度信息的返回结果
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("step_prompt", promptForNextStep);
        result.put("plan_id", plan.getPlanId());
        result.put("current_step_index", Integer.parseInt(plan.getCurrentStep()));
        result.put("total_steps", plan.getSteps().size());
        result.put("is_finished", plan.isFinished());
        result.put("step_status_history", plan.getStepStatus());
        result.put("task", plan.getTask());
        
        // 如果还有下一步，添加当前步骤描述
        if (!plan.isFinished() && Integer.parseInt(plan.getCurrentStep()) < plan.getSteps().size()) {
            result.put("current_step_description", plan.getSteps().get(Integer.parseInt(plan.getCurrentStep())));
        }

        return result;
    }

    public String think(OverAllState state) {

        String nextPrompt = (String) state.value("step_prompt").orElseThrow();

        if (nextPrompt.equalsIgnoreCase("Plan completed.")) {
            state.updateState(Map.of("final_output", state.value("step_output").orElseThrow()));
            return "end";
        }

        return "continue";
    }

    private PlanningTool.Plan parsePlan(String planJson) {
        planJson = extractJsonFromMarkdown(planJson);
        return JSONUtil.toBean(planJson, PlanningTool.Plan.class);
    }

    /**
     * 移除字符串中的Markdown代码块标记（```json 和 ```） 如果字符串不包含这些标记，则返回原始字符串
     * @param input 可能包含Markdown代码块标记的字符串
     * @return 去除了代码块标记的字符串
     */
    public static String extractJsonFromMarkdown(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // 正则表达式匹配 ```json ... ```
        Pattern pattern = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

}
