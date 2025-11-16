package com.coding.workflow.service.impl.processor;

import cn.hutool.json.JSONUtil;
import com.coding.core.common.Result;
import com.coding.workflow.enums.ErrorCodeEnum;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.manager.SandBoxManager;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.service.AbstractExecuteProcessor;
import com.coding.workflow.utils.AssertUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ScriptExecuteProcessor extends AbstractExecuteProcessor {

    private static final String SCRIPT_TYPE = "scriptType";
    private static final String SCRIPT_CONTENT = "scriptContent";

    @Resource
    private SandBoxManager sandBoxManager;

    // command + alt + u => 打开类图模版
    @Override
    public String getNodeType() {
        return NodeTypeEnum.SCRIPT.getCode();
    }

    @Override
    public String getNodeDescription() {
        return NodeTypeEnum.SCRIPT.getDesc();
    }

    @Override
    public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) throws InterruptedException {
        NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);
        AssertUtil.isNotNull(node.getConfig(), "节点配置不能为空");
        AssertUtil.isNotNull(node.getConfig().getNodeParam(), "节点配置参数不能为空");

        // 1. 获取节点参数
        String scriptType = node.getConfig().getNodeParam().get(SCRIPT_TYPE).toString();
        String scriptContent = node.getConfig().getNodeParam().get(SCRIPT_CONTENT).toString();

        // 2. 检查脚本类型
        if (!(ScriptType.python.name().equals(scriptType) || ScriptType.javascript.name().equals(scriptType))) {
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            nodeResult.setErrorCode(ErrorCodeEnum.WORKFLOW_CONFIG_ERROR.getCode());
            nodeResult.setErrorInfo("不支持的脚本类型: " + scriptType);
            return nodeResult;
        }

        // 3. 绑定脚本参数变量
        Map<String, Object> localVariableMap = constructScriptVariableMap(node, context);
        Map<String, Object> variableMap = Maps.newHashMap();
        variableMap.put("params", localVariableMap);

        // 4. 执行脚本
        Result<String> executeResult;
        scriptContent += "\nmain(params)";
        if (ScriptType.python.name().equals(scriptType)) {
            // 使用GraalVM执行python脚本
            executeResult = sandBoxManager.executePythonScript(scriptContent, variableMap, UUID.randomUUID().toString());
        } else {
            // 使用GraalVM执行javascript脚本
            executeResult = sandBoxManager.executeJavaScript(scriptContent, variableMap, UUID.randomUUID().toString());
        }

        // 5. 处理脚本执行结果
        if (executeResult != null && executeResult.isSuccess()) {
            ScriptExecutionResponse scriptRes = JSONUtil.toBean(executeResult.getData(), ScriptExecutionResponse.class);
            if (scriptRes.getSuccess()) {
                // 脚本执行成功
                Map<String, Object> outputParamsMap;
                if (scriptRes.getData() instanceof Map) {
                    // 处理输出结果
                    outputParamsMap = constructOutputParamsMap(node, scriptRes.getData(), context);
                    nodeResult.setInput(JSONUtil.toJsonStr(decorateInput(localVariableMap)));
                    nodeResult.setOutput(JSONUtil.toJsonStr(outputParamsMap));
                    nodeResult.setUsages(null);
                } else {
                    // 输出格式不满足指定格式
                    nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                    nodeResult.setErrorCode(ErrorCodeEnum.WORKFLOW_CONFIG_ERROR.getCode());
                    nodeResult.setErrorInfo("输出格式与配置不符, 原始数据: " + scriptRes.getData());
                }
            } else {
                // 脚本执行失败
                nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
                nodeResult.setErrorCode(ErrorCodeEnum.WORKFLOW_CONFIG_ERROR.getCode());
                nodeResult.setErrorInfo("脚本执行失败: " + scriptRes.getMessage());
            }
        } else {
            // 沙箱执行失败
            nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
            nodeResult.setErrorCode(ErrorCodeEnum.WORKFLOW_CONFIG_ERROR.getCode());
            String errorMsg = executeResult != null && !executeResult.isSuccess() ? executeResult.getMessage()
                    : "未知错误";
            nodeResult.setErrorInfo("脚本在沙箱中执行失败: " + errorMsg);
        }

        
        return nodeResult;
    }

    /**
     * 将input输入参数构建为input脚本输入Map
     *
     * @param node
     * @param context
     * @return
     */
    private Map<String, Object> constructScriptVariableMap(Node node, WorkflowContext context) {
        HashMap<String, Object> map = Maps.newHashMap();
        List<Node.InputParam> inputParams = node.getConfig().getInputParams();
        if (inputParams == null || inputParams.isEmpty()) {
            return map;
        }
        inputParams.forEach(inputParam -> {
            String valueFrom = inputParam.getValueFrom();
            if (ValueFromEnum.refer.name().equals(valueFrom)) {
                String value = getValueFromContext(inputParam, context);
                map.put(inputParam.getKey(), value);
            } else {
                if (inputParam.getValue() == null) {
                    return;
                }
                map.put(inputParam.getKey(), inputParam.getValue());
            }
        });

        return map;
    }

    public enum ScriptType {
        python, javascript
    }

    @Data
    @Accessors(chain = true)
    public static class ScriptExecutionResponse {

        @JsonProperty("data")
        private Object data;

        @JsonProperty("success")
        private Boolean success;

        @JsonProperty("message")
        private String message;

        @JsonProperty("code")
        private String code;

    }

}
