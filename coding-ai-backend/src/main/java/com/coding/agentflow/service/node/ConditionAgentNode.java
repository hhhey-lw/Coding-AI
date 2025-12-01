package com.coding.agentflow.service.node;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Branch;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 条件Agent节点
 * 基于Agent的智能决策进行条件分支判断
 */
@Slf4j
@Component
public class ConditionAgentNode extends AbstractNode {

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取并转换branches配置参数
        List<Branch> branches = getBranchesFromConfig(node);
        
        if (branches == null || branches.isEmpty()) {
            log.error("条件Agent节点branches配置为空或无效");
            return NodeExecutionResult.failure("branches配置为空或无效");
        }

        log.info("执行条件Agent节点，分支数量: {}, 分支详情: {}", branches.size(), JSONUtil.toJsonStr(branches));

        // 使用Agent进行智能条件判断
        String selectedLabel = evaluateConditionWithAgent(node, context, branches);

        log.info("条件Agent节点选择分支: {}", selectedLabel);

        // 返回带有选中分支label的结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("selectedLabel", selectedLabel);
        
        return NodeExecutionResult.success(resultData);
    }

    /**
     * 从节点配置中获取并转换branches列表
     */
    private List<Branch> getBranchesFromConfig(Node node) {
        // JSON对象
        Object branchesObj = getConfigParam(node, "branches");
        
        if (branchesObj == null) {
            log.warn("branches配置参数为null");
            return Collections.emptyList();
        }

        String jsonStr = (String) branchesObj;
        return JSONUtil.toList(jsonStr, Branch.class);
    }
    
    /**
     * 使用Agent评估条件
     * 1. 构建Agent的prompt，包含上下文信息和所有分支条件
     * 2. 调用LLM让Agent分析并选择最合适的分支
     * 3. 如果不符合任何label，返回默认分支label
     * 
     * @param node 节点配置
     * @param context 执行上下文
     * @param branches 分支列表
     * @return 选中的分支label
     */
    private String evaluateConditionWithAgent(Node node, Map<String, Object> context, List<Branch> branches) {
        // TODO: 实际的Agent条件评估逻辑
        // 这里应该:
        // 1. 构建Agent的prompt，包含上下文信息和所有分支条件
        // 2. 调用LLM让Agent分析并选择最合适的分支
        // 3. 解析Agent的响应，获取选中的分支label
        
        log.info("开始Agent条件评估，上下文: {}", JSONUtil.toJsonStr(context));
        
        // 临时实现：返回第一个分支的label
        // 实际应该调用Agent服务进行智能判断
        if (!branches.isEmpty()) {
            return branches.get(0).getLabel();
        }
        
        return null;
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        Object branchesObj = getConfigParam(node, "branches");
        if (branchesObj == null) {
            log.error("条件Agent节点缺少必需的branches配置");
            return false;
        }
        
        // 尝试解析branches
        List<Branch> branches = getBranchesFromConfig(node);
        if (branches.isEmpty()) {
            log.error("条件Agent节点branches配置为空或解析失败");
            return false;
        }
        
        // 验证每个分支都有label
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            if (branch.getLabel() == null || branch.getLabel().trim().isEmpty()) {
                log.error("条件Agent节点第{}个分支缺少label配置", i);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.CONDITION_AGENT.name();
    }
}
