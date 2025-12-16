package com.coding.workflow.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.coding.core.model.model.WorkflowNodeInstanceModel;
import com.coding.core.repository.WorkflowNodeInstanceRepository;
import com.coding.workflow.enums.NodeStatusEnum;
import com.coding.workflow.enums.NodeTypeEnum;
import com.coding.workflow.enums.ValueFromEnum;
import com.coding.workflow.enums.WorkflowStatusEnum;
import com.coding.workflow.exception.BizException;
import com.coding.workflow.model.workflow.Edge;
import com.coding.workflow.model.workflow.Node;
import com.coding.workflow.model.workflow.NodeResult;
import com.coding.workflow.model.workflow.WorkflowContext;
import com.coding.workflow.utils.JsonUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.coding.workflow.constants.WorkflowConstants.*;

@Slf4j
@Component
public abstract class AbstractExecuteProcessor implements ExecuteProcessor{

    /** 检查从上下文中提取变量值的表达式的格式：合法示例如下
     * user.name
     * items[0].price
     * data[0]._value
     * */
    private static final Pattern VALID_EXPRESSION_PATTERN = Pattern.compile("[0-9a-zA-Z\\-\\._\\[\\]]+");

    /** 匹配${var}格式的变量 */
    public static final Pattern VAR_EXPR_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    protected static final String INPUT_DECORATE_PARAM_KEY = "input";

    protected static final String OUTPUT_DECORATE_PARAM_KEY = "output";

    @Resource
    private WorkflowNodeInstanceRepository workflowNodeInstanceRepository;

    /**
     * 默认节点参数验证结果，直接返回成功
     */
    @Override
    public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
        CheckNodeParamResult result = CheckNodeParamResult.success();
        result.setNodeId(node.getId());
        result.setNodeName(node.getName());
        result.setNodeType(node.getType());
        return result;
    }

    @Override
    public void execute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        long startTime = System.currentTimeMillis();
        try {
            preCheck(graph, node, context);
            NodeResult nodeResult;
            try {
                log.info("开始执行节点，节点ID：{}", node.getId());
                nodeResult = innerExecute(graph, node, context);
            }
            catch (Exception e) {
                log.error("节点执行失败，节点ID：{}", node.getId(), e);
                nodeResult = NodeResult.error(node, e.getMessage());
            }
            // 额外功能暂时不实现
            handleVariables(graph, node, context, nodeResult);
            handleNodeResult(graph, node, context, nodeResult, startTime);
            finishNodeExecute(nodeResult);
            log.info("节点执行完成，节点ID：{}，结果：{}", node.getId(), JSONUtil.toJsonStr(nodeResult));
        }
        catch (Exception e) {
            log.error("节点执行失败，节点ID：{}", node.getId(), e);
            NodeResult errorNodeResult = NodeResult.error(node, e.getMessage());
            errorNodeResult.setInput(JSONUtil.toJsonStr(constructInputParamsMap(node, context)));
            handleNodeResult(graph, node, context, errorNodeResult, startTime);
        }
    }

    /**
     * 具体节点执行逻辑，由子类实现
     * @param graph 工作流图
     * @param node 执行节点
     * @param context 工作流上下文
     * @return
     */
    public abstract NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node,
                                            WorkflowContext context) throws InterruptedException;

    private static void preCheck(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
        // TODO 预检查
    }

    /**
     * 构建节点输入参数Map 从节点配置和上下文中
     * @param node 节点
     * @param context 工作流上下文
     * @return 输入参数Map
     */
    protected static Map<String, Object> constructInputParamsMap(Node node, WorkflowContext context) {
        Map<String, Object> map = Maps.newHashMap();
        List<Node.InputParam> inputParams = node.getConfig().getInputParams();
        if (CollectionUtils.isEmpty(inputParams)) {
            return map;
        }
        inputParams.stream().forEach(inputParam -> {
            String valueFrom = inputParam.getValueFrom();
            Object value = inputParam.getValue();
            if (valueFrom.equals(ValueFromEnum.refer.name())) {
                if (value == null) {
                    return;
                }
                Object finalValue = getValueFromContext(inputParam, context);
                if (finalValue == null) {
                    return;
                }
                map.put(inputParam.getKey(), finalValue);
            }
            else {
                if (value == null) {
                    return;
                }
                map.put(inputParam.getKey(), value);
            }
        });
        return map;
    }

    /**
     * 从执行结果中，构建节点输出参数Map
     * @param node 节点
     * @param resultObj 节点执行结果对象
     * @param context 工作流上下文
     * @return 输出参数Map
     */
    protected static Map<String, Object> constructOutputParamsMap(Node node, Object resultObj, WorkflowContext context) {
        Map<String, Object> resultMap = Maps.newHashMap();
        if (resultObj == null || resultObj == null) {
            return resultMap;
        }
        if (resultObj instanceof Map) {
            Map<String, Object> finalResultObj = new HashMap<>();
            constructOutputs((Map<String, Object>) resultObj, finalResultObj, node.getConfig().getOutputParams());
            return finalResultObj;
        } else {
            node.getConfig().getOutputParams().stream().forEach(param -> {
                resultMap.put(param.getKey(), resultObj);
            });
            return resultMap;
        }
    }

    /**
     * 从上下文获取值
     */
    protected static String getValueFromContext(Node.InputParam commonParam, WorkflowContext context) {
        if (commonParam == null || context == null) {
            return null;
        }
        String valueFrom = commonParam.getValueFrom();
        Object value = commonParam.getValue();

        if (StringUtils.isBlank(valueFrom)) {
            valueFrom = ValueFromEnum.refer.name();
        }

        if (value == null) {
            return null;
        }

        if (valueFrom.equals(ValueFromEnum.refer.name())) {
            String expression = String.valueOf(value).trim();
            if (expression.startsWith("${") && expression.endsWith("}")) {
                expression = expression.substring(2, expression.length() - 1);
            }
            if (StringUtils.isNotBlank(expression)) {
                Object ob = getValueFromPayload(expression, context.getVariablesMap());
                if (ob != null) {
                    return String.valueOf(ob);
                }
                throw new IllegalArgumentException("变量未找到: " + expression);
            }
        } else {
            return String.valueOf(value);
        }

        return null;
    }

    /**
     * OGNL表达式获取payload中的值
     */
    public static Object getValueFromPayload(String expression, Map<String, Object> payload) {
        if (StringUtils.isBlank(expression) || payload == null) {
            return null;
        }

        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1);
        }

        Matcher matcher = VALID_EXPRESSION_PATTERN.matcher(expression);
        if (matcher.matches()) {
            // 将[]转为{}，适配array下的获取逻辑
            String replaceExpression = expression.replaceAll("\\[", "\\{").replaceAll("\\]", "\\}");
            Object value;
            try {
                value = Ognl.getValue(replaceExpression, payload);
            }
            catch (OgnlException e) {
                log.error("从荷载中获取值失败, expression:{}, payload:{}", expression, payload, e);
                return null;
            }
            return value;
        }
        return expression;
    }


    /**
     * 处理节点执行结果中的output，放入上下文中
     * @param graph 工作流图
     * @param node 当前的执行节点
     * @param context 工作流上下文
     * @param nodeResult 节点执行结果
     */
    protected void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context, NodeResult nodeResult) throws InterruptedException {
        if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())
                || nodeResult.getNodeStatus().equals(NodeStatusEnum.EXECUTING.getCode())) {
            // 仅处理当前节点的输出结果，放入变量上下文中
            String outputJsonString = nodeResult.getOutput();
            if (StringUtils.isBlank(outputJsonString)) {
                return;
            }
            Map<String, Object> map = JsonUtils.fromJsonToMap(outputJsonString);
            if (CollectionUtil.isNotEmpty(map)) {
                context.getVariablesMap().put(node.getId(), map);
            }
        }
    }

    /**
     * 初始化节点执行结果，并刷新上下文
     * @param node 当前的执行节点
     * @param context 工作流上下文
     * @return 初始化的节点执行结果
     */
    protected NodeResult initNodeResultAndRefreshContext(Node node, WorkflowContext context) {
        NodeResult nodeResult = new NodeResult();
        nodeResult.setNodeId(node.getId());
        nodeResult.setNodeName(node.getName());
        nodeResult.setNodeType(node.getType());
        nodeResult.setUsages(null);
        nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
        context.getNodeResultMap().put(node.getId(), nodeResult);
        // 更新到库表
        WorkflowNodeInstanceModel nodeModel = buildWorkflowNodeModel(context, nodeResult);
        log.info("节点执行结果入库，节点ID：{}，结果：{}", node.getId(), JSONUtil.toJsonStr(nodeModel));
        workflowNodeInstanceRepository.add(nodeModel);
        return nodeResult;
    }

    /**
     * 完成节点执行，设置状态为成功
     * @param nodeResult 节点执行结果
     */
    private static void finishNodeExecute(NodeResult nodeResult) {
        if (nodeResult == null) {
            return;
        }
        if (nodeResult.getNodeStatus().equals(NodeStatusEnum.EXECUTING.getCode())) {
            nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
        }
    }

    /**
     * 构建输出内容
     * */
    private static void constructOutputs(Map<String, Object> sourceMap, Map<String, Object> targetMap,
                                         List<Node.OutputParam> outputParamsRefs) {
        Set<String> typeSet = new HashSet<>();
        typeSet.add(PARAM_TYPE_STRING_LOWER_CASE);
        typeSet.add(PARAM_TYPE_NUMBER_LOWER_CASE);
        typeSet.add(PARAM_TYPE_BOOLEAN_LOWER_CASE);
        if (sourceMap == null) {
            return;
        }
        if (CollectionUtils.isEmpty(outputParamsRefs)) {
            return;
        }
        // 移除最外层的output键
        sourceMap = (Map<String, Object>) sourceMap.get(OUTPUT_DECORATE_PARAM_KEY);
        for (Node.OutputParam outputParamsRef : outputParamsRefs) {
            String type = outputParamsRef.getType();
            if (type == null) {
                continue;
            }
            type = type.toLowerCase();
            if (typeSet.contains(type)) {
                if (PARAM_TYPE_STRING_LOWER_CASE.equals(type)) {
                    targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
                }
                else if (PARAM_TYPE_NUMBER_LOWER_CASE.equals(type)) {
                    targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
                }
                else if (PARAM_TYPE_BOOLEAN_LOWER_CASE.equals(type)) {
                    targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
                }
            }
            else if (PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE.equals(type)) {
                // list
                List list;
                if (!targetMap.containsKey(outputParamsRef.getKey())) {
                    list = new ArrayList();
                    targetMap.put(outputParamsRef.getKey(), list);
                }
                else {
                    list = (List) targetMap.get(outputParamsRef.getKey());
                }
                if (!CollectionUtils.isEmpty(outputParamsRef.getProperties())) {
                    constructArray((List) sourceMap.get(outputParamsRef.getKey()), list,
                            outputParamsRef.getProperties());
                }
                else {
                    list.addAll((List) sourceMap.get(outputParamsRef.getKey()));
                }
            }
            else if (type.startsWith("array")) {
                targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
            }
            else if (PARAM_TYPE_OBJECT_LOWER_CASE.equals(type)) {
                Map<String, Object> jsonObject;
                if (!targetMap.containsKey(outputParamsRef.getKey())) {
                    jsonObject = new HashMap<>();
                    targetMap.put(outputParamsRef.getKey(), jsonObject);
                }
                else {
                    jsonObject = (Map<String, Object>) targetMap.get(outputParamsRef.getKey());
                }
                if (!CollectionUtils.isEmpty(outputParamsRef.getProperties())) {
                    constructOutputs((Map<String, Object>) sourceMap.get(outputParamsRef.getKey()), jsonObject,
                            outputParamsRef.getProperties());
                }
                else {
                    // 如果object没有继续的属性，则不解析，直接将所有内容直接放入target对象中
                    jsonObject.putAll((Map<String, Object>) sourceMap.get(outputParamsRef.getKey()));
                }
            }
        }
    }

    /**
     * 构建数组内容
     * @param sourceList
     * @param targetList
     * @param outputParamsRefs
     */
    private static void constructArray(List sourceList, List targetList, List<Node.OutputParam> outputParamsRefs) {
        if (sourceList == null || outputParamsRefs == null) {
            return;
        }
        for (Node.OutputParam outputParamsRef : outputParamsRefs) {
            for (int i = 0; i < sourceList.size(); i++) {
                Object o = sourceList.get(i);
                Set<String> typeSet = new HashSet<>();
                typeSet.add(PARAM_TYPE_STRING_LOWER_CASE);
                typeSet.add(PARAM_TYPE_NUMBER_LOWER_CASE);
                typeSet.add(PARAM_TYPE_BOOLEAN_LOWER_CASE);
                String type = outputParamsRef.getType();
                if (type == null) {
                    continue;
                }
                type = type.toLowerCase();
                if (typeSet.contains(type)) {
                    if (o instanceof Map) {
                        Map<String, Object> oObj = (Map) o;
                        if (targetList.size() > i) {
                            ((Map) targetList.get(i)).put(outputParamsRef.getKey(), oObj.get(outputParamsRef.getKey()));
                        }
                        else {
                            Map<String, Object> newObj = new HashMap<>();
                            newObj.put(outputParamsRef.getKey(), oObj.get(outputParamsRef.getKey()));
                            targetList.add(newObj);
                        }
                    }
                    else {
                        targetList.add(o);
                    }
                }
                else if (PARAM_TYPE_OBJECT_LOWER_CASE.equals(type)) {
                    Map<String, Object> targetObj = new HashMap<>();
                    constructOutputs(((Map<String, Object>) ((Map<String, Object>) o).get(outputParamsRef.getKey())),
                            targetObj, outputParamsRef.getProperties());
                    if (targetList.size() > i) {
                        ((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(), targetObj);
                    }
                    else {
                        Map<String, Object> tempObj = new HashMap<>();
                        tempObj.put(outputParamsRef.getKey(), targetObj);
                        targetList.add(tempObj);
                    }
                }
                else if (type.equals(PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE)) {
                    List jsonArray = new ArrayList();
                    constructArray(((List) ((Map<String, Object>) o).get(outputParamsRef.getKey())), jsonArray,
                            outputParamsRef.getProperties());
                    if (targetList.size() > i) {
                        ((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(), jsonArray);
                    }
                    else {
                        Map<String, Object> tempObj = new HashMap<>();
                        tempObj.put(outputParamsRef.getKey(), jsonArray);
                        targetList.add(tempObj);
                    }
                }
                else if (type.startsWith("array")) {
                    if (targetList.size() > i) {
                        ((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(),
                                ((Map<String, Object>) o).get(outputParamsRef.getKey()));
                    }
                    else {
                        Map<String, Object> newObj = new HashMap<>();
                        newObj.put(outputParamsRef.getKey(), ((Map<String, Object>) o).get(outputParamsRef.getKey()));
                        targetList.add(newObj);
                    }
                }
            }
        }
    }

    /**
     * 更新节点执行结果，包括日志记录、状态更新等
     * @param graph 工作流图
     * @param node 当前的执行节点
     * @param context 工作流上下文
     * @param nodeResult 节点执行结果
     * @param startTime 节点开始执行时间
     */
    private void handleNodeResult(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context, NodeResult nodeResult, long startTime) {
        if (nodeResult == null) {
            return;
        }

        if (nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
            // 如果有节点执行失败，则整个任务失败
            context.setTaskStatus(WorkflowStatusEnum.FAIL.getCode());
            HashMap<String, Object> errorMap = Maps.newHashMap();
            errorMap.put("errorInfo", nodeResult.getErrorInfo());
            errorMap.put("nodeId", node.getId());
            errorMap.put("nodeName", node.getName());
            context.setErrorInfo(JsonUtils.toJson(errorMap));
        }
        nodeResult.setNodeName(node.getName() == null ? node.getId() : node.getName());
        nodeResult.setNodeExecuteTime((System.currentTimeMillis() - startTime) + "ms");

        if (!nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
            nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
        }
        context.getNodeResultMap().put(node.getId(), nodeResult);
        if (nodeResult.getNodeType().equals(NodeTypeEnum.END.getCode())
                && nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
            // 结束节点执行成功，设置任务为成功
            context.setTaskStatus(WorkflowStatusEnum.SUCCESS.getCode());
            // 并且设置任务结果为结束节点的输出
            context.setTaskResult(nodeResult.getOutput());
        }
        // 将节点结果存入数据库中
        WorkflowNodeInstanceModel nodeModel = buildWorkflowNodeModel(context, nodeResult);
        log.info("节点执行结果入库，节点ID：{}，结果：{}", node.getId(), JSONUtil.toJsonStr(nodeModel));
        workflowNodeInstanceRepository.updateByNodeId(nodeModel);
    }

    /**
     * 构建节点执行实例Model
     * @param context 工作流上下文
     * @param nodeResult 节点执行结果
     * @return 节点执行实例Model
     */
    private WorkflowNodeInstanceModel buildWorkflowNodeModel(WorkflowContext context, NodeResult nodeResult) {
        return WorkflowNodeInstanceModel.builder()
                .nodeId(nodeResult.getNodeId())
                .nodeName(nodeResult.getNodeName())
                .nodeType(nodeResult.getNodeType())
                .workflowInstanceId(context.getInstanceId())
                .status(nodeResult.getNodeStatus())
                .input(nodeResult.getInput())
                .output(nodeResult.getOutput())
                .startTime(nodeResult.getNodeStartTime())
                .executeTime(nodeResult.getNodeExecuteTime())
                .errorCode(nodeResult.getErrorCode())
                .errorInfo(nodeResult.getErrorInfo())
                .build();
    }

    /**
     * 替换模版中的变量
     * @param originalTemplate 原始模版内容
     * @param context 工作流上下文
     * @return 替换后的内容
     */
    public static String replaceTemplateContent(String originalTemplate, WorkflowContext context) {
        String promptContent = originalTemplate;
        // 1. 从模版中提取变量
        Matcher matcher = VAR_EXPR_PATTERN.matcher(originalTemplate);
        Set<String> keys = Sets.newHashSet();
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }

        // 2. 变量变量列表，从上下文中获取变量值并进行替换
        for (String key : keys) {
            Object o = getValueFromPayload(key, context.getVariablesMap());
            key = key.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
            if (o == null) {
                // 如果变量不存在，使用空字符串替换
                promptContent = promptContent.replaceAll("\\$\\{" + key + "}", "");
            } else {
                String replaceContent;
                if (o instanceof Map || o instanceof List) {
                    replaceContent = JsonUtils.toJson(o);
                } else {
                    replaceContent = String.valueOf(o);
                }
                // 将替换的文本 ``引用起来，避免$1，二次解析
                promptContent = promptContent.replaceAll("\\$\\{" + key + "}",
                        Matcher.quoteReplacement(replaceContent));
            }
        }
        return promptContent;
    }

    /**
     * 装饰输入参数 => {"input": input}
     * @param input 输入参数
     * @return 装饰后的输入参数Map
     */
    protected Map<String, Object> decorateInput(Object input) {
        Map<String, Object> outputObj = new HashMap<>();
        outputObj.put(INPUT_DECORATE_PARAM_KEY, input);
        return outputObj;
    }

}
