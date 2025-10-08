package com.coding.workflow.service.impl.processor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简化的分支判断处理器 - 抽离JudgeExecuteProcessor核心流转逻辑
 * 仅支持四种基本条件：空值、非空值、等于、不等于
 */
@Slf4j
@Component
public class JudgeExecuteProcessor extends AbstractExecuteProcessor {

    @Override
    public String getNodeType() {
        return NodeTypeEnum.JUDGE.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.JUDGE.getDesc();
    }

    /**
     * branchId => 目标nodeId
     *          => default => labelId !
     * 结果：MultiBranchReference
     */
    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        // 1. 初始化节点结果
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

        // 2. 获取分支配置
        NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
        Optional<Branch> any = config.getBranches().stream().filter(branch -> conditionHit(branch, context)).findFirst();

        if (any.isPresent()) {
            Branch branch = any.get();
            List<String> targetIds = null;
            Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
            if (CollectionUtil.isNotEmpty(edges)) {
                targetIds = edges.stream()
//                        .filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_" + branch.getId()))
                        .filter(edge -> edge.getTarget().equals(node.getId()))
                        .map(Edge::getTarget)
                        .toList();
            }
            nodeResult.setInput(JSONUtil.toJsonStr(Map.of(INPUT_DECORATE_PARAM_KEY, config)));
            Map<String, Object> outputObj = new HashMap<>();
            if (CollectionUtil.isNotEmpty(targetIds)) {
                String targetIdString = targetIds.stream().collect(Collectors.joining(","));
                outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "命中分支: " + branch.getId() + "，流转到节点: " + targetIdString);
                // nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
            } else {
                outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "命中分支: " + branch.getId() + "，但没有配置后续节点");
                nodeResult.setErrorInfo("命中分支: " + branch.getId() + "，但没有配置后续节点");
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            }
            nodeResult.setInput(constructInput(config.getBranches(), context));
            nodeResult.setOutput(JsonUtils.toJson(outputObj));
            nodeResult.setMultiBranch(true);
            NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
            branchReference.setConditionId(branch.getId());
            branchReference.setTargetIds(targetIds);
            nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
        } else {
            Optional<Branch> defaultOptional = config.getBranches()
                    .stream()
                    .filter(branch -> "ELSE".equals(branch.getLabel()))
                    .findFirst();
            if (defaultOptional.isPresent()) {
                Branch branch = defaultOptional.get();
                //List<String> targetIds = null;
                //Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
                //if (CollectionUtil.isNotEmpty(edges)) {
                //    targetIds = edges.stream()
                //            .filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_default"))
                //            .map(Edge::getTarget)
                //            .collect(Collectors.toList());
                //}
                List<String> targetIds = List.of(branch.getId());
                Map<String, Object> outputObj = new HashMap<>();
                if (CollectionUtil.isNotEmpty(targetIds)) {
                    String targetIdString = targetIds.stream().collect(Collectors.joining(","));
                    outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "命中分支目标节点: " + targetIdString);
                }
                else {
                    outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "默认分支目标节点为空");
                    nodeResult.setErrorInfo("默认分支目标节点为空");
                    nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                }
                nodeResult.setNodeId(branch.getId());
                nodeResult.setOutput(JsonUtils.toJson(outputObj));
                nodeResult.setMultiBranch(true);
                NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
                branchReference.setConditionId(branch.getId());
                branchReference.setTargetIds(targetIds);
                nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
            } else {
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorInfo("没有命中任何分支");
            }
        }
        return nodeResult;
    }

    // 构建input
    private String constructInput(List<Branch> originBranches, WorkflowContext context) {
        List<Branch> branches = originBranches.stream()
                .filter(branch -> !branch.getId().equals("default"))
                .toList();
        ArrayList<Map<String, Object>> inputList = Lists.newArrayList();
        // 遍历每个条件分支, 默认使用特殊情况，多branch但每个branch只有一个condition
        // 条件在节点中进行判断： 条件参数：
        // branch1 => conditions: [ {leftKey, operator, rightValue}, ...], logic: AND/OR
        // branch2 => conditions: [ {leftKey, operator, rightValue}, ...], logic: AND/OR
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            Map<String, Object> branchObj = Maps.newHashMap();
            ArrayList<Map<String, Object>> subBranchObj = Lists.newArrayList();
            branch.getConditions().forEach(condition -> {
                Map<String, Object> conditionObj = Maps.newHashMap();
                Node.InputParam leftKey = condition.getLeftKey();
                String leftValue = getValueFromContext(leftKey, context);
                // leftKey:${nodeId.param} op constant_value
                conditionObj.put("leftKey", leftKey);
                conditionObj.put("leftValue", leftValue);
                conditionObj.put("operator", condition.getOperator());
                conditionObj.put("rightValue", condition.getRightValue());
                subBranchObj.add(conditionObj);
            });
            branchObj.put("conditionId", branch.getId());
            branchObj.put("subBranches", subBranchObj);
            branchObj.put("logic", branch.getLogic());
            branchObj.put("label", "Condition " + (i + 1));
            inputList.add(branchObj);
        }
        return JsonUtils.toJson(inputList);
    }

    /**
     * 判断分支条件是否命中
     */
    private boolean conditionHit(Branch branch, WorkflowContext context) {
        // 默认分支不参与命中判断
        if ("ELSE".equals(branch.getLabel())) {
            return false;
        }

        String logic = branch.getLogic();
        // 默认逻辑是AND
        if (StringUtils.isBlank(logic)) {
            logic = Logic.AND.name();
        }

        List<Condition> conditions = branch.getConditions();
        if (CollectionUtil.isEmpty(conditions)) {
            // 如果没有配置条件，认为不命中
            return false;
        }

        return checkCondition(logic, conditions, context);
    }

    /**
     * 检查条件列表
     */
    private boolean checkCondition(String logic, List<Condition> conditions, WorkflowContext context) {
        Logic logicEnum = Logic.valueOf(logic.toUpperCase());

        switch (logicEnum) {
            case AND:
                // AND逻辑：所有条件都必须满足
                for (Condition condition : conditions) {
                    Node.InputParam leftKey = condition.getLeftKey();
                    String operator = condition.getOperator();

                    String leftValue = getValueFromContext(leftKey, context);
                    String rightValue = condition.getRightValue();

                    if (!evaluateCondition(leftKey, leftValue, rightValue, operator)) {
                        return false;
                    }
                }
                return true;

            case OR:
                // OR逻辑：至少一个条件满足
                for (Condition condition : conditions) {
                    Node.InputParam leftKey = condition.getLeftKey();
                    String operator = condition.getOperator();

                    String leftValue = getValueFromContext(leftKey, context);
                    String rightValue = condition.getRightValue();

                    if (evaluateCondition(leftKey, leftValue, rightValue, operator)) {
                        return true;
                    }
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * 评估单个条件 - 仅支持四种基本操作符
     */
    private boolean evaluateCondition(Node.InputParam leftKey, String leftValue, String rightValue, String operator) {
        String leftType = leftKey.getType();

        if (StringUtils.isBlank(leftType) || StringUtils.isBlank(operator)) {
            return false;
        }

        Set<String> operatorScopeSet = Set.of("equals", "notEquals", "isNull", "isNotNull");
        Set<String> typeSet = Set.of("string", "number", "boolean");
        if (!operatorScopeSet.contains(operator)) {
            return false;
        }
        if (StringUtils.isBlank(leftType) || !typeSet.contains(leftType.toLowerCase())) {
            return false;
        }

        try {
            switch (operator) {
                case "equals":
                    if ("string".equalsIgnoreCase(leftType)) {
                        return leftValue.equals(rightValue);
                    }
                    else if ("number".equalsIgnoreCase(leftType)) {
                        return compareDigits(leftValue, rightValue) == 0;
                    }
                    else if ("boolean".equalsIgnoreCase(leftType)) {
                        return Boolean.parseBoolean(leftValue) == Boolean.parseBoolean(rightValue);
                    }
                    return false;

                case "notEquals":
                    if ("string".equalsIgnoreCase(leftType)) {
                        return !leftValue.equals(rightValue);
                    }
                    else if ("number".equalsIgnoreCase(leftType)) {
                        return compareDigits(leftValue, rightValue) != 0;
                    }
                    else if ("boolean".equalsIgnoreCase(leftType)) {
                        return (Boolean.parseBoolean(leftValue) != Boolean.parseBoolean(rightValue));
                    }
                    return false;

                case "isNull":
                    if ("string".equalsIgnoreCase(leftType)) {
                        return StringUtils.isEmpty(leftValue);
                    }
                    else if ("number".equalsIgnoreCase(leftType)) {
                        return StringUtils.isEmpty(leftValue);
                    }
                    else if ("boolean".equalsIgnoreCase(leftType)) {
                        return StringUtils.isEmpty(leftValue);
                    }
                    return false;

                case "isNotNull":
                    if ("string".equalsIgnoreCase(leftType)) {
                        return !StringUtils.isEmpty(leftValue);
                    }
                    else if ("number".equalsIgnoreCase(leftType)) {
                        return true;
                    }
                    else if ("boolean".equalsIgnoreCase(leftType)) {
                        return true;
                    }
                    return false;

                default:
                    log.warn("不支持的操作: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            log.error("执行条件判断错出: leftValue={}, rightValue={}, operator={}",
                     leftValue, rightValue, operator, e);
            return false;
        }
    }

    private int compareDigits(String leftValue, String rightValue) {
        return new BigDecimal(leftValue).compareTo(new BigDecimal(rightValue));
    }

    // 枚举定义
    private enum Logic {
        AND, OR
    }

    @Data
    public static class NodeParam {

        private List<Branch> branches;

    }

    // 数据模型
    @Data
    public static class Branch {
        private String id;                    // targetId
        private String label;          // 分支标签
        private String logic;         // 逻辑类型：AND/OR
        private List<Condition> conditions;   // 条件列表
    }

    @Data
    public static class Condition {
        @JsonProperty("left_key")
        private Node.InputParam leftKey;      // 左操作数变量名
        private String operator;     // 操作符：isNull, isNotNull, equals, notEquals
        @JsonProperty("right_value")
        private String rightValue;   // 右操作数值（直接值，不是变量引用）
    }
}
